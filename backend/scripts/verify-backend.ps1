[CmdletBinding()]
param(
    [string]$BaseUrl = "http://localhost:8080",
    [int]$HealthTimeoutSeconds = 60
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$javaHome = $env:JAVA_HOME
if ([string]::IsNullOrWhiteSpace($javaHome) -or -not (Test-Path (Join-Path $javaHome "bin\java.exe"))) {
    throw "JAVA_HOME must point to a Java 17 installation"
}
$previousErrorActionPreference = $ErrorActionPreference
try {
    # java -version writes to stderr; avoid PowerShell treating that informational
    # stream as a terminating NativeCommandError under ErrorActionPreference=Stop.
    $ErrorActionPreference = "Continue"
    $javaVersion = (& (Join-Path $javaHome "bin\java.exe") -version 2>&1 | Out-String)
} finally {
    $ErrorActionPreference = $previousErrorActionPreference
}
if ($javaVersion -notmatch 'version "17\.') {
    throw "Java 17 is required. Detected: $javaVersion"
}

Push-Location (Join-Path $PSScriptRoot "..")
$appProcess = $null
try {
    & .\mvnw.cmd -q clean verify -DskipITs
    if ($LASTEXITCODE -ne 0) { throw "Maven verification failed with exit code $LASTEXITCODE" }

    $jar = Join-Path (Get-Location) "target\gymmind-backend-0.0.1-SNAPSHOT.jar"
    if (-not (Test-Path $jar)) { throw "Backend jar not found: $jar" }
    $log = Join-Path (Get-Location) "target\verify-backend.log"
    $errorLog = Join-Path (Get-Location) "target\verify-backend-error.log"
    Remove-Item $log,$errorLog -Force -ErrorAction SilentlyContinue
    $appProcess = Start-Process -FilePath (Join-Path $javaHome "bin\java.exe") `
        -ArgumentList @("-jar", $jar) -WorkingDirectory (Get-Location) `
        -RedirectStandardOutput $log -RedirectStandardError $errorLog -PassThru
} finally { Pop-Location }

$health = "$BaseUrl/actuator/health"
$deadline = (Get-Date).AddSeconds($HealthTimeoutSeconds)
do {
    try {
        $response = Invoke-RestMethod -Uri $health -Method Get -TimeoutSec 3
        if ($response.status -eq "UP") {
            if ($appProcess -and -not $appProcess.HasExited) {
                $appProcess | Stop-Process -Force -ErrorAction SilentlyContinue
            }
            Write-Output "GymMind backend health: UP"
            exit 0
        }
    } catch { }
    Start-Sleep -Seconds 2
} while ((Get-Date) -lt $deadline)
if ($appProcess -and -not $appProcess.HasExited) {
    $appProcess | Stop-Process -Force -ErrorAction SilentlyContinue
}
$detail = if (Test-Path $log) { (Get-Content $log -Tail 40 | Out-String) } else { "no startup log" }
$detail += if (Test-Path $errorLog) { "`n" + (Get-Content $errorLog -Tail 40 | Out-String) } else { "" }
throw "GymMind backend health check timed out: $health`n$detail"
