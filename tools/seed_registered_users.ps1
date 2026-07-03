param(
    [int]$UserCount = 1000,
    [int]$VisitorsPerUser = 2,
    [string]$AdminBaseUrl = "http://127.0.0.1:8002",
    [string]$Password = "LiveStart123",
    [string]$PhonePrefix = "1879000",
    [string]$FixedIdCard = "445121200404047420",
    [string]$BulkCsvPath = "D:\02_Workspace\Projects\Livestart\jmeter\users_bulk_registered_1000.csv",
    [string]$Scenario3CsvPath = "D:\02_Workspace\Projects\Livestart\jmeter\users_scenario3_registered_1000.csv",
    [string]$MySqlExe = "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe",
    [string]$MySqlHost = "127.0.0.1",
    [int]$MySqlPort = 3306,
    [string]$MySqlUser = "root",
    [string]$MySqlPassword = "123456",
    [int]$SkuPoolSize = 12,
    [int]$StartIndex = 1,
    [switch]$ForceSendCodeLogin,
    [switch]$ContinueOnUserError
)

$ErrorActionPreference = "Stop"

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
        "-p$MySqlPassword",
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

function New-Username([int]$index) {
    return "reg_user_{0:D4}" -f $index
}

function New-RealName([int]$index) {
    return "Registered User {0:D4}" -f $index
}

function Invoke-JsonPost {
    param(
        [string]$Url,
        [object]$Body,
        [hashtable]$Headers = @{}
    )

    $jsonBody = $Body | ConvertTo-Json -Depth 6 -Compress
    return Invoke-RestMethod -Method Post -Uri $Url -Headers $Headers -Body $jsonBody -ContentType "application/json; charset=UTF-8"
}

function Get-ErrorResponseText {
    param(
        [System.Management.Automation.ErrorRecord]$ErrorRecord
    )

    if ($null -eq $ErrorRecord) {
        return $null
    }
    if ($ErrorRecord.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($ErrorRecord.Exception.Response.GetResponseStream())
        return $reader.ReadToEnd()
    }
    return $ErrorRecord.ToString()
}

Write-Section "Step 1 - Load Active SKU Pool"
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

Write-Section "Step 2 - Register Users Through Real API"
Ensure-ParentDirectory $BulkCsvPath
Ensure-ParentDirectory $Scenario3CsvPath

[System.IO.File]::WriteAllLines($BulkCsvPath, @("phone,userId,username,visitorId,skuId"), [System.Text.UTF8Encoding]::new($false))
[System.IO.File]::WriteAllLines($Scenario3CsvPath, @("phone,userId,username,visitorId,skuId"), [System.Text.UTF8Encoding]::new($false))

Write-Host "[INFO] UserCount=$UserCount StartIndex=$StartIndex VisitorsPerUser=$VisitorsPerUser" -ForegroundColor Cyan
Write-Host "[INFO] BulkCsvPath=$BulkCsvPath" -ForegroundColor Cyan
Write-Host "[INFO] Scenario3CsvPath=$Scenario3CsvPath" -ForegroundColor Cyan

$successCount = 0
$failedUsers = New-Object System.Collections.Generic.List[string]
$endIndex = $StartIndex + $UserCount - 1

for ($index = $StartIndex; $index -le $endIndex; $index++) {
    $phone = New-Phone $index

    try {
        $username = New-Username $index
        $realName = New-RealName $index

        $registerBody = [ordered]@{
            username = $username
            password = $Password
            realName = $realName
            phone = $phone
            idCard = $FixedIdCard
        }

        try {
            $registerResp = Invoke-JsonPost -Url "$AdminBaseUrl/api/live-start/admin/v1/user" -Body $registerBody
        } catch {
            if (-not $ForceSendCodeLogin) {
                $detail = Get-ErrorResponseText $_
                throw "[FAIL] register user failed for phone=$phone detail=$detail"
            }
        }

        $token = $null
        if ($registerResp -and [string]::Equals([string]$registerResp.code, "0")) {
            $token = $registerResp.data.token
        }

        if (-not $token) {
            Invoke-RestMethod -Method Post -Uri "$AdminBaseUrl/api/live-start/admin/v1/user/send-code?phone=$phone" | Out-Null
            $loginResp = Invoke-RestMethod -Method Post -Uri "$AdminBaseUrl/api/live-start/admin/v1/user/login/code?phone=$phone&code=888888"
            if (-not [string]::Equals([string]$loginResp.code, "0")) {
                throw "[FAIL] login/code failed for phone=$phone code=$($loginResp.code) message=$($loginResp.message)"
            }
        }

        $meResp = Invoke-RestMethod -Method Get -Uri "$AdminBaseUrl/api/live-start/admin/v1/user/$phone"
        if (-not [string]::Equals([string]$meResp.code, "0")) {
            throw "[FAIL] query user by phone failed for phone=$phone"
        }
        $userId = [Int64]$meResp.data.id
        $currentUsername = [string]$meResp.data.username

        $headers = @{
            userId = $userId.ToString()
            username = $currentUsername
            phone = $phone
            userType = "1"
        }

        $visitorIds = New-Object System.Collections.Generic.List[Int64]
        for ($visitorIndex = 1; $visitorIndex -le $VisitorsPerUser; $visitorIndex++) {
            $visitorBody = [ordered]@{
                realName = "{0} Visitor {1}" -f $realName, $visitorIndex
                cardType = 1
                cardNo = $FixedIdCard
                mobile = $phone
            }

            try {
                Invoke-JsonPost -Url "$AdminBaseUrl/api/live-start/admin/v1/visitor" -Body $visitorBody -Headers $headers | Out-Null
            } catch {
                # Duplicate visitor identity per user is allowed here; existing visitors are read below.
            }
        }

        $visitorListResp = Invoke-RestMethod -Method Get -Uri "$AdminBaseUrl/api/live-start/admin/v1/visitor/list" -Headers $headers
        if (-not [string]::Equals([string]$visitorListResp.code, "0")) {
            throw "[FAIL] query visitor list failed for phone=$phone"
        }
        foreach ($visitor in @($visitorListResp.data)) {
            $visitorIds.Add([Int64]$visitor.id)
        }
        if ($visitorIds.Count -eq 0) {
            throw "[FAIL] no visitor created for phone=$phone"
        }

        $skuId = $skuPool[($index - $StartIndex) % $skuPool.Count]
        $csvLine = "{0},{1},{2},{3},{4}" -f $phone, $userId, $currentUsername, $visitorIds[0], $skuId
        Add-Content -LiteralPath $BulkCsvPath -Value $csvLine -Encoding UTF8
        Add-Content -LiteralPath $Scenario3CsvPath -Value $csvLine -Encoding UTF8
        $successCount++
        Write-Host "[OK] registered phone=$phone userId=$userId visitorId=$($visitorIds[0]) skuId=$skuId" -ForegroundColor Green
    } catch {
        $message = $_ | Out-String
        $failedUsers.Add("$phone`t$message")
        Write-Host "[FAIL] phone=$phone $message" -ForegroundColor Red
        if (-not $ContinueOnUserError) {
            throw
        }
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
