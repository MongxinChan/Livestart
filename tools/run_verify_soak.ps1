param(
    [string]$TargetUrl = "http://127.0.0.1:8004/api/engine/order/verify",
    [int]$DurationMinutes = 120,
    [int]$IntervalSeconds = 1,
    [string]$LogPath = "engine-verify-soak.log"
)

$ErrorActionPreference = "Continue"
$headers = @{
    userId = "soak-reader"
    username = "soak"
    phone = "13800138009"
    userType = "4"
    "Content-Type" = "application/json"
}
$body = '{"checkCode":"SOAK-NONEXISTENT"}'
$endTime = (Get-Date).AddMinutes($DurationMinutes)
$total = 0
$http200Responses = 0
$limitedResponses = 0
$errors = 0

Add-Content -LiteralPath $LogPath -Value "START $(Get-Date -Format o) durationMinutes=$DurationMinutes intervalSeconds=$IntervalSeconds"
while ((Get-Date) -lt $endTime) {
    try {
        $response = Invoke-WebRequest -UseBasicParsing -Uri $TargetUrl -Method Post -Headers $headers -Body $body -TimeoutSec 15
        if ($response.StatusCode -eq 200) {
            $http200Responses++
        } else {
            $errors++
        }
    } catch {
        if ($_.Exception.Response -and [int]$_.Exception.Response.StatusCode -eq 429) {
            $limitedResponses++
        } else {
            $errors++
        }
    }

    $total++
    if (($total % 60) -eq 0) {
        Add-Content -LiteralPath $LogPath -Value "$(Get-Date -Format o) total=$total http200=$http200Responses limited=$limitedResponses errors=$errors"
    }
    Start-Sleep -Seconds $IntervalSeconds
}

Add-Content -LiteralPath $LogPath -Value "END $(Get-Date -Format o) total=$total http200=$http200Responses limited=$limitedResponses errors=$errors"
