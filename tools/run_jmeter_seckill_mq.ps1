param(
    [string]$JMeterBin = "",
    [string]$JdkHome = "",
    [string]$TargetHost = "127.0.0.1",
    [int]$EnginePort = 8004,
    [string]$Protocol = "http",
    [string]$UsersCsvPath = "",
    [int]$TgThreads = 100,
    [int]$TgRampTime = 1,
    [ValidateRange(0, 1000000)]
    [int]$TgLoops = 0,
    [int]$UserType = 1,
    [string]$JMeterHeap = "-Xms256m -Xmx512m -XX:MaxMetaspaceSize=256m",
    [string]$JmxPath = "",
    [string]$ResultFile = "",
    [string]$ReportDir = ""
)

$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot

if ([string]::IsNullOrWhiteSpace($JmxPath)) {
    $JmxPath = Join-Path $root "jmeter\livestart_seckill_create_order_mq.jmx"
}
if ([string]::IsNullOrWhiteSpace($UsersCsvPath)) {
    $UsersCsvPath = Join-Path $root "jmeter\users_bulk_seckill_smoke_5.csv"
}
if ([string]::IsNullOrWhiteSpace($ResultFile)) {
    $ResultFile = Join-Path $root "jmeter\results-seckill-mq.jtl"
}
if ([string]::IsNullOrWhiteSpace($ReportDir)) {
    $ReportDir = Join-Path $root "jmeter\html_report_seckill_mq"
}

function Write-Section($message) {
    Write-Host ""
    Write-Host "==================================================" -ForegroundColor Cyan
    Write-Host $message -ForegroundColor Cyan
    Write-Host "==================================================" -ForegroundColor Cyan
}

function Resolve-InputPath($path, $basePath, $mustExist) {
    if ([string]::IsNullOrWhiteSpace($path)) {
        return $null
    }

    $candidate = if ([System.IO.Path]::IsPathRooted($path)) {
        $path
    } else {
        Join-Path $basePath $path
    }

    if ($mustExist) {
        return (Resolve-Path -LiteralPath $candidate).Path
    }

    return [System.IO.Path]::GetFullPath($candidate)
}

function Assert-PathExists($path, $label) {
    if (-not (Test-Path -LiteralPath $path)) {
        throw "[FAIL] Missing ${label}: $path"
    }
    Write-Host "[OK] ${label}: $path" -ForegroundColor Green
}

function Reset-Directory($path) {
    if (Test-Path -LiteralPath $path) {
        Remove-Item -LiteralPath $path -Recurse -Force
    }
    New-Item -ItemType Directory -Path $path -Force | Out-Null
}

function Find-JMeterBin($explicitPath) {
    if (-not [string]::IsNullOrWhiteSpace($explicitPath)) {
        return Resolve-InputPath $explicitPath $root $true
    }

    if (-not [string]::IsNullOrWhiteSpace($env:JMETER_HOME)) {
        $fromHome = Join-Path $env:JMETER_HOME "bin\jmeter.bat"
        if (Test-Path -LiteralPath $fromHome) {
            return (Resolve-Path -LiteralPath $fromHome).Path
        }
    }

    $fromPath = Get-Command "jmeter.bat" -ErrorAction SilentlyContinue
    if ($null -ne $fromPath) {
        return $fromPath.Source
    }

    $fromPath = Get-Command "jmeter" -ErrorAction SilentlyContinue
    if ($null -ne $fromPath) {
        return $fromPath.Source
    }

    $fallbacks = @(
        "D:\03_Software\Apache\jmeter-5.6.3\bin\jmeter.bat",
        "C:\apache-jmeter-5.6.3\bin\jmeter.bat"
    )
    foreach ($fallback in $fallbacks) {
        if (Test-Path -LiteralPath $fallback) {
            return (Resolve-Path -LiteralPath $fallback).Path
        }
    }

    throw "[FAIL] JMeter not found. Set -JMeterBin or JMETER_HOME, for example: .\tools\run_jmeter_seckill_mq.ps1 -JMeterBin D:\apache-jmeter-5.6.3\bin\jmeter.bat"
}

function Resolve-Java($explicitJdkHome) {
    if (-not [string]::IsNullOrWhiteSpace($explicitJdkHome)) {
        $home = Resolve-InputPath $explicitJdkHome $root $true
        $java = Join-Path $home "bin\java.exe"
        Assert-PathExists $java "JDK java.exe"
        return @{ Home = $home; Java = $java }
    }

    if (-not [string]::IsNullOrWhiteSpace($env:JAVA_HOME)) {
        $java = Join-Path $env:JAVA_HOME "bin\java.exe"
        if (Test-Path -LiteralPath $java) {
            return @{ Home = (Resolve-Path -LiteralPath $env:JAVA_HOME).Path; Java = (Resolve-Path -LiteralPath $java).Path }
        }
    }

    $fromPath = Get-Command "java.exe" -ErrorAction SilentlyContinue
    if ($null -ne $fromPath) {
        return @{ Home = ""; Java = $fromPath.Source }
    }

    throw "[FAIL] Java not found. Set -JdkHome or JAVA_HOME to a JDK/JRE that can run JMeter."
}

function Assert-CsvShape($path) {
    $header = Get-Content -LiteralPath $path -TotalCount 1
    $expected = "phone,userId,username,visitorId,skuId"
    if ($header -ne $expected) {
        throw "[FAIL] CSV header mismatch. Expected '$expected', got '$header': $path"
    }

    $records = @(Import-Csv -LiteralPath $path)
    if ($records.Count -eq 0) {
        throw "[FAIL] CSV has header but no data rows: $path"
    }

    $dataRowCount = $records.Count
    $invalidRows = @($records | Where-Object {
        [string]::IsNullOrWhiteSpace($_.phone) -or
        [string]::IsNullOrWhiteSpace($_.userId) -or
        [string]::IsNullOrWhiteSpace($_.username) -or
        [string]::IsNullOrWhiteSpace($_.visitorId) -or
        [string]::IsNullOrWhiteSpace($_.skuId) -or
        $_.userId -notmatch '^\d+$' -or
        $_.visitorId -notmatch '^\d+$' -or
        $_.skuId -notmatch '^\d+$'
    })
    if ($invalidRows.Count -gt 0) {
        throw "[FAIL] CSV contains $($invalidRows.Count) invalid data rows: $path"
    }
    $duplicateUserRows = @($records | Group-Object userId | Where-Object Count -gt 1)
    if ($duplicateUserRows.Count -gt 0) {
        throw "[FAIL] CSV contains duplicate userId groups ($($duplicateUserRows.Count)): $path"
    }
    Write-Host "[OK] CSV shape: $path (data rows=$dataRowCount; each row used once)" -ForegroundColor Green
    return $dataRowCount
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

function Invoke-CheckedProcess($filePath, $arguments) {
    Write-Host "[RUN] $filePath $($arguments -join ' ')" -ForegroundColor Yellow
    & $filePath @arguments
    $exitCode = $LASTEXITCODE
    if ($exitCode -ne 0) {
        throw "[FAIL] Command failed with exit code ${exitCode}: $filePath"
    }
}

$JmxPath = Resolve-InputPath $JmxPath $root $true
$UsersCsvPath = Resolve-InputPath $UsersCsvPath $root $true
$ResultFile = Resolve-InputPath $ResultFile $root $false
$ReportDir = Resolve-InputPath $ReportDir $root $false
$JMeterBin = Find-JMeterBin $JMeterBin
$javaInfo = Resolve-Java $JdkHome

Write-Section "Step 1 - Check Seckill MQ JMeter Assets"
Assert-PathExists $JMeterBin "JMeter executable"
Assert-PathExists $JmxPath "Seckill MQ JMX"
Assert-PathExists $UsersCsvPath "Users CSV"
$csvRowCount = Assert-CsvShape $UsersCsvPath
Assert-TcpPort -HostName $TargetHost -Port $EnginePort -Label "Engine API"
if ($TgThreads -lt 1) {
    throw "[FAIL] TgThreads must be greater than zero"
}
$autoLoops = $TgLoops -eq 0
if ($TgLoops -eq 0) {
    $TgLoops = [Math]::Max(1, [int][Math]::Ceiling($csvRowCount / [double]$TgThreads))
    Write-Host "[OK] Auto loops: $TgLoops (threads=$TgThreads, csv rows=$csvRowCount)" -ForegroundColor Green
}
$plannedSamples = [Math]::Min($TgThreads, $csvRowCount) * $TgLoops
if (-not $autoLoops -and $plannedSamples -gt $csvRowCount) {
    Write-Warning "Planned samples ($plannedSamples) exceed CSV rows ($csvRowCount). JMX stops when CSV is exhausted; use -TgLoops 0 for automatic sizing."
}

$javaVersion = & $javaInfo.Java -version 2>&1 | Select-Object -First 1
Write-Host "[OK] Java: $($javaInfo.Java)" -ForegroundColor Green
Write-Host "[OK] Java version: $javaVersion" -ForegroundColor Green

Write-Section "Step 2 - Prepare Report Output"
$resultDir = Split-Path -Parent $ResultFile
if (-not (Test-Path -LiteralPath $resultDir)) {
    New-Item -ItemType Directory -Path $resultDir -Force | Out-Null
}
if (Test-Path -LiteralPath $ResultFile) {
    Remove-Item -LiteralPath $ResultFile -Force
}

$rawResultFile = [System.IO.Path]::Combine(
    $resultDir,
    ([System.IO.Path]::GetFileNameWithoutExtension($ResultFile) + "-raw" + [System.IO.Path]::GetExtension($ResultFile))
)
if (Test-Path -LiteralPath $rawResultFile) {
    Remove-Item -LiteralPath $rawResultFile -Force
}
Reset-Directory $ReportDir

Write-Section "Step 3 - Run Seckill MQ Test"
if (-not [string]::IsNullOrWhiteSpace($javaInfo.Home)) {
    $env:JAVA_HOME = $javaInfo.Home
    $env:PATH = "$($javaInfo.Home)\bin;$env:PATH"
}
$env:HEAP = $JMeterHeap
$env:NEW = "-XX:NewSize=128m -XX:MaxNewSize=128m"
$env:JVM_ARGS = "-Xss256k"

Write-Host "[OK] Target: ${Protocol}://${TargetHost}:${EnginePort}" -ForegroundColor Green
Write-Host "[OK] Threads/Ramp/Loops: ${TgThreads}/${TgRampTime}/${TgLoops}" -ForegroundColor Green
Write-Host "[OK] JMeter heap: $env:HEAP" -ForegroundColor Green

$runArgs = @(
    "-n",
    "-t", $JmxPath,
    "-l", $rawResultFile,
    "-JHOST=$TargetHost",
    "-JENGINE_PORT=$EnginePort",
    "-JPROTO=$Protocol",
    "-JSECKILL_CSV=$UsersCsvPath",
    "-JTG_THREADS=$TgThreads",
    "-JTG_RAMP_TIME=$TgRampTime",
    "-JTG_LOOPS=$TgLoops",
    "-JUSER_TYPE=$UserType",
    "-Jjmeter.save.saveservice.response_data=false",
    "-Jjmeter.save.saveservice.response_data.on_error=true",
    "-Jjmeter.save.saveservice.samplerData=false",
    "-Jjmeter.save.saveservice.requestHeaders=false",
    "-Jjmeter.save.saveservice.responseHeaders=false"
)
Invoke-CheckedProcess $JMeterBin $runArgs

Write-Section "Step 4 - Build Main Transaction Report"
$rows = @(Import-Csv -LiteralPath $rawResultFile)
$mainRows = @($rows | Where-Object { $_.label -eq "Seckill Create Order By MQ" })
if ($mainRows.Count -eq 0) {
    throw "[FAIL] No parent transaction rows found in raw JTL: $rawResultFile"
}
$successfulMainRows = @($mainRows | Where-Object { $_.success -eq "true" })
if ($successfulMainRows.Count -eq 0) {
    $failureSummary = ($mainRows | Group-Object responseCode | Sort-Object Count -Descending | Select-Object -First 3 | ForEach-Object { "$($_.Name)=$($_.Count)" }) -join ", "
    throw "[FAIL] All parent transactions failed. $failureSummary. Raw JTL: $rawResultFile"
}

$rawLines = Get-Content -LiteralPath $rawResultFile
$header = $rawLines | Select-Object -First 1
$mainLines = @($rawLines | Select-Object -Skip 1 | Where-Object { $_ -match ',Seckill Create Order By MQ,' })
[System.IO.File]::WriteAllLines($ResultFile, @($header) + $mainLines, [System.Text.UTF8Encoding]::new($false))

$reportArgs = @(
    "-g", $ResultFile,
    "-o", $ReportDir
)
Invoke-CheckedProcess $JMeterBin $reportArgs

Write-Section "Seckill MQ Test Complete"
Write-Host "Raw JTL: $rawResultFile" -ForegroundColor Green
Write-Host "JTL: $ResultFile" -ForegroundColor Green
Write-Host "HTML Report: $ReportDir\index.html" -ForegroundColor Green
