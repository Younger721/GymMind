# GymMind Docker infrastructure stop (UTF-8 BOM)
$ErrorActionPreference = "Stop"
$ProjectRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
Set-Location $ProjectRoot

Write-Host "======================================" -ForegroundColor White
Write-Host " Stop GymMind Docker Infrastructure" -ForegroundColor White
Write-Host "======================================" -ForegroundColor White

function Invoke-Compose([string[]]$ComposeArgs) {
    $prevErrorAction = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    & docker compose @ComposeArgs
    $code = $LASTEXITCODE
    if ($code -ne 0) {
        & docker-compose @ComposeArgs
        $code = $LASTEXITCODE
    }
    $ErrorActionPreference = $prevErrorAction
    return $code
}

$removeVolumes = ($args -contains "-v") -or ($args -contains "--volumes")
$downArgs = @("--env-file", ".env", "--profile", "mysql", "down")
if ($removeVolumes) {
    $downArgs += "-v"
    Write-Host "Stopping containers and removing volumes..." -ForegroundColor Yellow
} else {
    Write-Host "Stopping containers (volumes kept)..." -ForegroundColor Cyan
}

$code = Invoke-Compose $downArgs
if ($code -eq 0) {
    Write-Host "Done." -ForegroundColor Green
} else {
    Write-Host "Stop failed." -ForegroundColor Red
    exit 1
}
