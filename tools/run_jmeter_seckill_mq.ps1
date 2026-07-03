param(
    [string]$JMeterBin = "D:\03_Software\Apache\jmeter-5.6.3\bin\jmeter.bat",
    [string]$TargetHost = "127.0.0.1",
    [int]$EnginePort = 8004,
    [string]$UsersCsvPath = "D:\02_Workspace\Projects\Livestart\jmeter\users_scenario3_registered_1000.csv",
    [int]$TgThreads = 100,
    [int]$TgRampTime = 1,
    [int]$TgLoops = 400,
    [string]$JdkHome = "C:\Program Files\Java\jdk-17",
    [string]$JMeterHeap = "-Xms256m -Xmx512m -XX:MaxMetaspaceSize=256m",
    [string]$ResultFile = "D:\02_Workspace\Projects\Livestart\jmeter\results-seckill-mq.jtl",
    [string]$ReportDir = "D:\02_Workspace\Projects\Livestart\jmeter\html_report_seckill_mq"
)

$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$jmxPath = Join-Path $root "jmeter\livestart_seckill_create_order_mq.jmx"
$staticUsersCsvPath = Join-Path $root "jmeter\users_seckill_fresh_5000.csv"

function Write-Section($message) {
    Write-Host ""
    Write-Host "==================================================" -ForegroundColor Cyan
    Write-Host $message -ForegroundColor Cyan
    Write-Host "==================================================" -ForegroundColor Cyan
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
    New-Item -ItemType Directory -Path $path | Out-Null
}

Write-Section "Step 1 - Check Seckill JMeter Assets"
Assert-PathExists $JMeterBin "JMeter executable"
Assert-PathExists $jmxPath "Seckill JMeter script"
Assert-PathExists $UsersCsvPath "Prebuilt users CSV"
Assert-PathExists (Join-Path $JdkHome "bin\java.exe") "JDK for JMeter"

Write-Section "Step 2 - Prepare Static CSV For JMeter"
if ((Resolve-Path -LiteralPath $UsersCsvPath).Path -ne (Resolve-Path -LiteralPath $staticUsersCsvPath).Path) {
    Copy-Item -LiteralPath $UsersCsvPath -Destination $staticUsersCsvPath -Force
    Write-Host "[OK] Copied users CSV to static JMeter path: $staticUsersCsvPath" -ForegroundColor Green
} else {
    Write-Host "[OK] Static users CSV already selected: $staticUsersCsvPath" -ForegroundColor Green
}

Write-Section "Step 3 - Prepare Report Output"
$resultDir = Split-Path -Parent $ResultFile
if (-not (Test-Path -LiteralPath $resultDir)) {
    New-Item -ItemType Directory -Path $resultDir | Out-Null
}
if (Test-Path -LiteralPath $ResultFile) {
    Remove-Item -LiteralPath $ResultFile -Force
}
Reset-Directory $ReportDir

Write-Section "Step 4 - Run Seckill MQ Test"
$env:JAVA_HOME = $JdkHome
$env:PATH = "$JdkHome\bin;$env:PATH"
$env:HEAP = $JMeterHeap
$env:NEW = "-XX:NewSize=128m -XX:MaxNewSize=128m"
$env:JVM_ARGS = "-Xss256k"
Write-Host "[OK] JMeter heap: $env:HEAP" -ForegroundColor Green

$rawResultFile = [System.IO.Path]::Combine(
    (Split-Path -Parent $ResultFile),
    ([System.IO.Path]::GetFileNameWithoutExtension($ResultFile) + "-raw" + [System.IO.Path]::GetExtension($ResultFile))
)
if (Test-Path -LiteralPath $rawResultFile) {
    Remove-Item -LiteralPath $rawResultFile -Force
}

$command = @(
    $JMeterBin,
    "-n",
    "-t", "`"$jmxPath`"",
    "-l", "`"$rawResultFile`"",
    "-JHOST=$TargetHost",
    "-JENGINE_PORT=$EnginePort",
    "-JTG_THREADS=$TgThreads",
    "-JTG_RAMP_TIME=$TgRampTime",
    "-JTG_LOOPS=$TgLoops",
    "-JUSER_TYPE=1"
) -join " "

Write-Host "[RUN] $command" -ForegroundColor Yellow
cmd /c $command
$exitCode = $LASTEXITCODE
if ($exitCode -ne 0) {
    throw "[FAIL] JMeter execution failed with exit code: $exitCode"
}

Write-Section "Step 5 - Build Main Seckill Report"
$rows = @(Import-Csv -LiteralPath $rawResultFile)
$mainRows = @($rows | Where-Object { $_.label -eq "Seckill Create Order By MQ" })
if ($mainRows.Count -eq 0) {
    throw "[FAIL] No parent transaction rows found in raw JTL: $rawResultFile"
}
$rawLines = Get-Content -LiteralPath $rawResultFile
$header = $rawLines | Select-Object -First 1
$mainLines = @($rawLines | Select-Object -Skip 1 | Where-Object { $_ -match ',Seckill Create Order By MQ,' })
[System.IO.File]::WriteAllLines($ResultFile, @($header) + $mainLines, [System.Text.UTF8Encoding]::new($false))

$reportCommand = @(
    $JMeterBin,
    "-g", "`"$ResultFile`"",
    "-o", "`"$ReportDir`""
) -join " "

Write-Host "[RUN] $reportCommand" -ForegroundColor Yellow
cmd /c $reportCommand
$exitCode = $LASTEXITCODE
if ($exitCode -ne 0) {
    throw "[FAIL] JMeter report generation failed with exit code: $exitCode"
}

Write-Section "Seckill MQ Test Complete"
Write-Host "Raw JTL: $rawResultFile" -ForegroundColor Green
Write-Host "JTL: $ResultFile" -ForegroundColor Green
Write-Host "HTML Report: $ReportDir\index.html" -ForegroundColor Green
