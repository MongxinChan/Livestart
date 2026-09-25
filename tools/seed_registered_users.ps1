param(
    [ValidateRange(1, 100000)]
    [int]$UserCount = 1000,
    [ValidateRange(1, 10)]
    [int]$VisitorsPerUser = 1,
    [ValidateRange(1, 32)]
    [int]$Parallelism = 8,
    [ValidateRange(5, 300)]
    [int]$RequestTimeoutSec = 30,
    [string]$AdminBaseUrl = "http://127.0.0.1:8002",
    [string]$SmsCode = "888888",
    [string]$PhonePrefix = "1879000",
    [string]$FixedIdCard = "445121200404047420",
    [string]$BulkCsvPath = "",
    [string]$Scenario3CsvPath = "",
    [string]$MySqlExe = "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe",
    [string]$MySqlHost = "127.0.0.1",
    [ValidateRange(1, 65535)]
    [int]$MySqlPort = 3306,
    [string]$MySqlUser = "root",
    [string]$MySqlPassword = $env:LIVESTART_DB_PASSWORD,
    [ValidateRange(1, 1000)]
    [int]$SkuPoolSize = 12,
    [ValidateRange(1, 999999)]
    [int]$StartIndex = 1,
    [switch]$ContinueOnUserError,
    [switch]$ShowRows
)

$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
if ($PhonePrefix -notmatch '^\d+$') {
    throw "[FAIL] PhonePrefix must contain digits only"
}
if (($PhonePrefix.Length + ([Math]::Max($StartIndex + $UserCount - 1, 1)).ToString().Length) -gt 11) {
    throw "[FAIL] Generated phone numbers exceed 11 digits. Adjust PhonePrefix, StartIndex, or UserCount."
}
if ([string]::IsNullOrWhiteSpace($BulkCsvPath)) {
    $BulkCsvPath = Join-Path $projectRoot "jmeter\users_bulk_registered_${UserCount}.csv"
}
if ([string]::IsNullOrWhiteSpace($Scenario3CsvPath)) {
    $Scenario3CsvPath = Join-Path $projectRoot "jmeter\users_scenario3_registered_${UserCount}.csv"
}

function Write-Section($message) {
    Write-Host ""
    Write-Host "==================================================" -ForegroundColor Cyan
    Write-Host $message -ForegroundColor Cyan
    Write-Host "==================================================" -ForegroundColor Cyan
}

function Ensure-ParentDirectory($path) {
    $parent = Split-Path -Parent $path
    if (-not [string]::IsNullOrWhiteSpace($parent) -and -not (Test-Path -LiteralPath $parent)) {
        New-Item -ItemType Directory -Path $parent | Out-Null
    }
}

function Invoke-MySqlQuery {
    param(
        [string]$Database,
        [string]$Query
    )

    $args = @(
        "--host=$MySqlHost",
        "--port=$MySqlPort",
        "-u$MySqlUser",
        "--password=$MySqlPassword",
        "-D", $Database,
        "-N",
        "-e", $Query
    )

    & $MySqlExe @args
    if ($LASTEXITCODE -ne 0) {
        throw "[FAIL] mysql query failed for database=$Database"
    }
}

function New-Phone([int]$index) {
    return "{0}{1:D4}" -f $PhonePrefix, $index
}

function Assert-TcpPort {
    param([string]$HostName, [int]$Port, [string]$Label)

    $client = [System.Net.Sockets.TcpClient]::new()
    try {
        $task = $client.ConnectAsync($HostName, $Port)
        if (-not $task.Wait(2000) -or -not $client.Connected) {
            throw "[FAIL] $Label is unreachable: ${HostName}:${Port}"
        }
        Write-Host "[OK] $Label ${HostName}:${Port}" -ForegroundColor Green
    } catch {
        if ($_.Exception.Message.StartsWith('[FAIL]')) { throw }
        throw "[FAIL] $Label is unreachable: ${HostName}:${Port}"
    } finally {
        $client.Dispose()
    }
}

Write-Section "Step 1 - Load Active SKU Pool"
if (-not (Test-Path -LiteralPath $MySqlExe)) {
    throw "[FAIL] MySQL executable not found: $MySqlExe"
}
Assert-TcpPort -HostName $MySqlHost -Port $MySqlPort -Label "MySQL"
$adminUri = [Uri]$AdminBaseUrl
Assert-TcpPort -HostName $adminUri.Host -Port $adminUri.Port -Label "Admin API"
$skuQuery = @"
SELECT s.id
FROM t_ticket_sku s
INNER JOIN t_event e ON e.id = s.event_id
WHERE s.remaining_stock > 0
  AND e.status = 2
ORDER BY s.remaining_stock DESC, s.selling_price DESC, s.id ASC
LIMIT $SkuPoolSize;
"@
$skuRows = Invoke-MySqlQuery -Database "live_start" -Query $skuQuery
if (-not $skuRows -or $skuRows.Count -eq 0) {
    throw "[FAIL] No active sku rows loaded from live_start.t_ticket_sku"
}
$skuPool = @($skuRows | ForEach-Object { [Int64]($_ -split "\s+")[0] })

Write-Section "Step 2 - Login Or Register Users Through Verification Code API"
Ensure-ParentDirectory $BulkCsvPath
Ensure-ParentDirectory $Scenario3CsvPath

[System.IO.File]::WriteAllLines($BulkCsvPath, @("phone,userId,username,visitorId,skuId"), [System.Text.UTF8Encoding]::new($false))
[System.IO.File]::WriteAllLines($Scenario3CsvPath, @("phone,userId,username,visitorId,skuId"), [System.Text.UTF8Encoding]::new($false))

Write-Host "[INFO] UserCount=$UserCount StartIndex=$StartIndex VisitorsPerUser=$VisitorsPerUser Parallelism=$Parallelism" -ForegroundColor Cyan
Write-Host "[INFO] BulkCsvPath=$BulkCsvPath" -ForegroundColor Cyan
Write-Host "[INFO] Scenario3CsvPath=$Scenario3CsvPath" -ForegroundColor Cyan

$endIndex = $StartIndex + $UserCount - 1

$seedOneUser = {
    $ErrorActionPreference = "Stop"
    $index = [int]$_

    function Invoke-JsonPostInParallel {
        param([string]$Url, [object]$Body, [hashtable]$Headers = @{})
        $jsonBody = $Body | ConvertTo-Json -Depth 6 -Compress
        return Invoke-RestMethod -Method Post -Uri $Url -Headers $Headers -Body $jsonBody -ContentType "application/json; charset=UTF-8" -TimeoutSec $using:RequestTimeoutSec
    }

    try {
        $phone = "$using:PhonePrefix$('{0:D4}' -f $index)"
        $realName = "Registered User {0:D4}" -f $index
        $seedClientIp = "192.0.2.{0}" -f (($index % 254) + 1)
        $sendCodeHeaders = @{ "X-Livestart-Client-IP" = $seedClientIp }

        Invoke-RestMethod -Method Post -Uri "$using:AdminBaseUrl/api/live-start/admin/v1/user/send-code?phone=$phone" -Headers $sendCodeHeaders -TimeoutSec $using:RequestTimeoutSec | Out-Null
        $loginResp = Invoke-JsonPostInParallel -Url "$using:AdminBaseUrl/api/live-start/admin/v1/user/login/code" -Body ([ordered]@{ phone = $phone; code = $using:SmsCode })
        if (-not [string]::Equals([string]$loginResp.code, "0")) {
            throw "login/code failed: code=$($loginResp.code) message=$($loginResp.message)"
        }

        # UserController exposes /me, not /user/{phone}. Resolve the sharding key from the global phone map,
        # then send identity headers so the direct admin service can load the user profile.
        $mappingArgs = @(
            "--host=$using:MySqlHost",
            "--port=$using:MySqlPort",
            "-u$using:MySqlUser",
            "--password=$using:MySqlPassword",
            "-D", "live_start",
            "-N",
            "-e", "SELECT user_id FROM t_user_phone_mapping WHERE phone='$phone' LIMIT 1"
        )
        $mappingRows = @(& $using:MySqlExe @mappingArgs)
        if ($LASTEXITCODE -ne 0) {
            throw "phone mapping query failed"
        }
        if ($mappingRows.Count -eq 0) {
            throw "phone mapping not found after login"
        }
        $mappedUserId = [Int64](($mappingRows[0] -split "\s+")[0])
        $meHeaders = @{
            userId = $mappedUserId.ToString()
            phone = $phone
        }
        $meResp = Invoke-RestMethod -Method Get -Uri "$using:AdminBaseUrl/api/live-start/admin/v1/user/me" -Headers $meHeaders -TimeoutSec $using:RequestTimeoutSec
        if (-not [string]::Equals([string]$meResp.code, "0")) {
            throw "query user by phone failed"
        }
        $userId = [Int64]$meResp.data.id
        $currentUsername = [string]$meResp.data.username
        $headers = @{
            userId = $userId.ToString()
            username = $currentUsername
            phone = $phone
            userType = "1"
        }

        for ($visitorIndex = 1; $visitorIndex -le $using:VisitorsPerUser; $visitorIndex++) {
            $visitorBody = [ordered]@{
                realName = "{0} Visitor {1}" -f $realName, $visitorIndex
                cardType = 1
                cardNo = $using:FixedIdCard
                mobile = $phone
            }
            try {
                Invoke-JsonPostInParallel -Url "$using:AdminBaseUrl/api/live-start/admin/v1/visitor" -Body $visitorBody -Headers $headers | Out-Null
            } catch {
                # 重复观演人继续读取已有记录。
            }
        }

        $visitorListResp = Invoke-RestMethod -Method Get -Uri "$using:AdminBaseUrl/api/live-start/admin/v1/visitor/list" -Headers $headers -TimeoutSec $using:RequestTimeoutSec
        if (-not [string]::Equals([string]$visitorListResp.code, "0")) {
            throw "query visitor list failed"
        }
        $visitor = @($visitorListResp.data) | Select-Object -First 1
        if ($null -eq $visitor) {
            throw "no visitor created"
        }

        $skuValues = $using:skuPool
        $skuId = $skuValues[($index - $using:StartIndex) % $skuValues.Count]
        [pscustomobject]@{
            Index = $index
            Success = $true
            Line = "{0},{1},{2},{3},{4}" -f $phone, $userId, $currentUsername, ([Int64]$visitor.id), $skuId
            Error = $null
        }
    } catch {
        [pscustomobject]@{
            Index = $index
            Success = $false
            Line = $null
            Error = $_.Exception.Message
        }
    }
}

$results = @($StartIndex..$endIndex | ForEach-Object -Parallel $seedOneUser -ThrottleLimit $Parallelism)
$successRows = @($results | Where-Object Success | Sort-Object Index | ForEach-Object Line)
$failedUsers = @($results | Where-Object { -not $_.Success } | Sort-Object Index | ForEach-Object { "$(New-Phone $_.Index)`t$($_.Error)" })
$successCount = $successRows.Count

$outputLines = @("phone,userId,username,visitorId,skuId") + $successRows
[System.IO.File]::WriteAllLines($BulkCsvPath, $outputLines, [System.Text.UTF8Encoding]::new($false))
[System.IO.File]::WriteAllLines($Scenario3CsvPath, $outputLines, [System.Text.UTF8Encoding]::new($false))

if ($ShowRows) {
    foreach ($row in $results | Sort-Object Index) {
        if ($row.Success) {
            Write-Host "[OK] $($row.Line)" -ForegroundColor Green
        } else {
            Write-Host "[FAIL] $(New-Phone $row.Index) $($row.Error)" -ForegroundColor Red
        }
    }
} else {
    foreach ($row in $results | Where-Object { -not $_.Success } | Sort-Object Index) {
        Write-Host "[FAIL] $(New-Phone $row.Index) $($row.Error)" -ForegroundColor Red
    }
}

Write-Section "Completed"
Write-Host "Requested users: $UserCount" -ForegroundColor Green
Write-Host "Successful users: $successCount" -ForegroundColor Green
Write-Host "CSV: $BulkCsvPath" -ForegroundColor Green
Write-Host "CSV: $Scenario3CsvPath" -ForegroundColor Green
if ($failedUsers.Count -gt 0) {
    $failedPath = Join-Path (Split-Path -Parent $Scenario3CsvPath) "users_seed_failed.txt"
    Set-Content -LiteralPath $failedPath -Value $failedUsers -Encoding UTF8
    Write-Host "Failed users: $($failedUsers.Count), details: $failedPath" -ForegroundColor Yellow
}
if ($failedUsers.Count -gt 0 -and -not $ContinueOnUserError) {
    throw "[FAIL] $($failedUsers.Count) users failed. Use -ContinueOnUserError to keep partial CSV."
}
