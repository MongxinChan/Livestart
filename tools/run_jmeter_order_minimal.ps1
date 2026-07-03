param(
    [string]$JMeterBin = "D:\03_Software\Apache\jmeter-5.6.3\bin\jmeter.bat",
    [string]$TargetHost = "localhost",
    [int]$AdminPort = 8002,
    [int]$EnginePort = 8004,
    [string]$Scenario3UsersCsvPath = "D:\02_Workspace\Projects\Livestart\jmeter\users_order_smoke.csv",
    [int]$TgThreads = 1,
    [int]$TgRampTime = 1,
    [int]$TgLoops = 1,
    [string]$JdkHome = "C:\Program Files\Java\jdk-17",
    [string]$JmxPath = "",
    [string]$ResultFile = "D:\02_Workspace\Projects\Livestart\jmeter\results-order-minimal.jtl",
    [string]$ReportDir = "D:\02_Workspace\Projects\Livestart\jmeter\html_report_order_minimal"
)

$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
if ([string]::IsNullOrWhiteSpace($JmxPath)) {
    $JmxPath = Join-Path $root "jmeter\livestart_order_create_minimal_fixed.jmx"
}

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

Write-Section "Step 1 - Check Minimal JMeter Assets"
Assert-PathExists $JMeterBin "JMeter executable"
Assert-PathExists $JmxPath "Minimal JMeter script"
Assert-PathExists $Scenario3UsersCsvPath "Scenario3 CSV"
Assert-PathExists (Join-Path $JdkHome "bin\java.exe") "JDK for JMeter"

Write-Section "Step 2 - Prepare Report Output"
$resultDir = Split-Path -Parent $ResultFile
if (-not (Test-Path -LiteralPath $resultDir)) {
    New-Item -ItemType Directory -Path $resultDir | Out-Null
}
if (Test-Path -LiteralPath $ResultFile) {
    Remove-Item -LiteralPath $ResultFile -Force
}
Reset-Directory $ReportDir

Write-Section "Step 3 - Run Minimal Order Flow Test"
$env:JAVA_HOME = $JdkHome
$env:PATH = "$JdkHome\bin;$env:PATH"

$command = @(
    $JMeterBin,
    "-n",
    "-t", "`"$JmxPath`"",
    "-l", "`"$ResultFile`"",
    "-e",
    "-o", "`"$ReportDir`"",
    "-JHOST=$TargetHost",
    "-JADMIN_PORT=$AdminPort",
    "-JENGINE_PORT=$EnginePort",
    "-JSCENARIO3_CSV=$Scenario3UsersCsvPath",
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

Write-Section "Minimal Test Complete"
Write-Host "JTL: $ResultFile" -ForegroundColor Green
Write-Host "HTML Report: $ReportDir\index.html" -ForegroundColor Green
