param(
    [string]$RocketMqHome = "F:\Tool\rocketmq-all-5.3.2-source-release\distribution\target\rocketmq-5.3.2\rocketmq-5.3.2",
    [string]$NameServer = "127.0.0.1:9876",
    [string]$JavaHome = "C:\Program Files\Java\jdk-17"
)

$ErrorActionPreference = "Stop"

function Get-RocketMqCommandPath {
    param(
        [string]$BaseDir,
        [string]$CommandName
    )

    $candidates = @(
        (Join-Path $BaseDir "bin\$CommandName.cmd"),
        (Join-Path $BaseDir "bin\tools\$CommandName.cmd"),
        (Join-Path $BaseDir $CommandName),
        (Join-Path $BaseDir "bin\$CommandName")
    )

    foreach ($candidate in $candidates) {
        if (Test-Path -LiteralPath $candidate) {
            return $candidate
        }
    }

    return $null
}

function Wait-PortListening {
    param(
        [int]$Port,
        [int]$RetrySeconds = 20
    )

    for ($i = 0; $i -lt $RetrySeconds; $i++) {
        $listening = Get-NetTCPConnection -State Listen -ErrorAction SilentlyContinue |
            Where-Object { $_.LocalPort -eq $Port } |
            Select-Object -First 1
        if ($listening) {
            return $true
        }
        Start-Sleep -Seconds 1
    }

    return $false
}

function Ensure-ListeningPort {
    param(
        [int]$Port,
        [string]$DisplayName,
        [scriptblock]$StartAction
    )

    $existing = Get-NetTCPConnection -State Listen -ErrorAction SilentlyContinue |
        Where-Object { $_.LocalPort -eq $Port } |
        Select-Object -First 1

    if ($existing) {
        Write-Host "$DisplayName is already listening on port $Port."
        return
    }

    & $StartAction

    if (-not (Wait-PortListening -Port $Port)) {
        throw "$DisplayName failed to start on port $Port."
    }

    Write-Host "$DisplayName started on port $Port."
}

function Start-RocketMqCmd {
    param(
        [string]$CommandPath,
        [string[]]$Arguments,
        [switch]$Wait
    )

    if (-not (Test-Path -LiteralPath $JavaHome)) {
        throw "JAVA_HOME not found: $JavaHome"
    }

    $cmdArgs = @(
        "/c",
        "set JAVA_HOME=$JavaHome&& set ROCKETMQ_HOME=$RocketMqHome&& set PATH=$JavaHome\bin;%PATH%&& call `"$CommandPath`" $($Arguments -join ' ')"
    )

    if ($Wait) {
        $process = Start-Process -FilePath "cmd.exe" -ArgumentList $cmdArgs -WorkingDirectory (Split-Path -Parent $CommandPath) -WindowStyle Hidden -PassThru -Wait
        if ($process.ExitCode -ne 0) {
            throw "Command failed: $CommandPath $($Arguments -join ' ')"
        }
        return
    }

    Start-Process -FilePath "cmd.exe" -ArgumentList $cmdArgs -WorkingDirectory (Split-Path -Parent $CommandPath) -WindowStyle Hidden | Out-Null
}

function Reset-RocketMqCorruptedTimerMetrics {
    param(
        [string]$StoreRootDir
    )

    $configDir = Join-Path $StoreRootDir "config"
    $targets = @(
        (Join-Path $configDir "timermetrics"),
        (Join-Path $configDir "timermetrics.bak")
    )

    foreach ($target in $targets) {
        if (-not (Test-Path -LiteralPath $target)) {
            continue
        }

        $fileInfo = Get-Item -LiteralPath $target -ErrorAction SilentlyContinue
        if (-not $fileInfo) {
            continue
        }

        $shouldReset = $false

        if ($fileInfo.Length -eq 0) {
            $shouldReset = $true
        } else {
            try {
                $content = Get-Content -LiteralPath $target -Raw -ErrorAction Stop
                if ([string]::IsNullOrWhiteSpace($content)) {
                    $shouldReset = $true
                } else {
                    $trimmed = $content.Trim()
                    if (-not $trimmed.StartsWith("{") -or -not $trimmed.EndsWith("}")) {
                        $shouldReset = $true
                    }
                }
            } catch {
                $shouldReset = $true
            }
        }

        if ($shouldReset) {
            $backupPath = "$target.corrupted.$((Get-Date).ToString('yyyyMMddHHmmss')).bak"
            Move-Item -LiteralPath $target -Destination $backupPath -Force
            Write-Host "Reset corrupted RocketMQ timer metrics file: $target"
        }
    }
}

$nameSrvCmd = Get-RocketMqCommandPath -BaseDir $RocketMqHome -CommandName "mqnamesrv"
$brokerCmd = Get-RocketMqCommandPath -BaseDir $RocketMqHome -CommandName "mqbroker"
$mqAdminCmd = Get-RocketMqCommandPath -BaseDir $RocketMqHome -CommandName "mqadmin"
$brokerConf = Join-Path $PSScriptRoot "rocketmq-dev-broker.conf"
$storeRootDir = "F:\tmp\rocketmq-store-livestart"

if (-not $nameSrvCmd) {
    throw "mqnamesrv not found under $RocketMqHome"
}
if (-not $brokerCmd) {
    throw "mqbroker not found under $RocketMqHome"
}
if (-not $mqAdminCmd) {
    throw "mqadmin not found under $RocketMqHome"
}
if (-not (Test-Path -LiteralPath $brokerConf)) {
    throw "broker config not found: $brokerConf"
}

foreach ($dir in @(
    $storeRootDir,
    (Join-Path $storeRootDir "commitlog"),
    (Join-Path $storeRootDir "consumequeue"),
    (Join-Path $storeRootDir "index"),
    (Join-Path $storeRootDir "config")
)) {
    if (-not (Test-Path -LiteralPath $dir)) {
        New-Item -ItemType Directory -Path $dir -Force | Out-Null
    }
}

Reset-RocketMqCorruptedTimerMetrics -StoreRootDir $storeRootDir

Ensure-ListeningPort -Port 9876 -DisplayName "RocketMQ NameServer" -StartAction {
    Start-RocketMqCmd -CommandPath $nameSrvCmd -Arguments @()
}

Ensure-ListeningPort -Port 10911 -DisplayName "RocketMQ Broker" -StartAction {
    Start-RocketMqCmd -CommandPath $brokerCmd -Arguments @("-n", $NameServer, "-c", "`"$brokerConf`"")
}

$topics = @(
    "livestart_engine_order-create_topic",
    "livestart_engine_order-pay-success_topic",
    "livestart_engine_order-delay-close_topic",
    "livestart_distribution_commission-settle_topic",
    "livestart_distribution_ticket-task-execute_topic"
)

foreach ($topic in $topics) {
    Start-RocketMqCmd -CommandPath $mqAdminCmd -Arguments @("updateTopic", "-n", $NameServer, "-c", "DefaultCluster", "-t", $topic) -Wait
    Write-Host "Topic ensured: $topic"
}

Write-Host "RocketMQ local dev environment is ready."
