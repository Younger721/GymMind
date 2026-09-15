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
$javaVersion = (& (Join-Path $javaHome "bin\java.exe") -version 2>&1 | Out-String)
if ($javaVersion -notmatch 'version "17\.') {
    throw "Java 17 is required. Detected: $javaVersion"
}

Push-Location (Join-Path $PSScriptRoot "..")
try {
    & .\mvnw.cmd -q clean verify -DskipITs
    if ($LASTEXITCODE -ne 0) { throw "Maven verification failed with exit code $LASTEXITCODE" }
} finally { Pop-Location }

$health = "$BaseUrl/actuator/health"
$deadline = (Get-Date).AddSeconds($HealthTimeoutSeconds)
do {
    try {
        $response = Invoke-RestMethod -Uri $health -Method Get -TimeoutSec 3
        if ($response.status -eq "UP") {
            Write-Output "GymMind backend health: UP"
            exit 0
        }
    } catch { }
    Start-Sleep -Seconds 2
} while ((Get-Date) -lt $deadline)
throw "GymMind backend health check timed out: $health"
