[CmdletBinding()]
param(
    [string]$HostName = "localhost",
    [int]$Port = 3306,
    [Parameter(Mandatory = $true)]
    [string]$UserName,
    [switch]$Execute,
    [string]$Confirmation
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$confirmationPhrase = "DROP gymmind"
$dropSql = "DROP DATABASE IF EXISTS gymmind;"
$createSql = "CREATE DATABASE gymmind CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;"

if (-not $Execute) {
    Write-Output "Dry-run only. No database was changed."
    Write-Output "Target: gymmind"
    Write-Output $dropSql
    Write-Output $createSql
    Write-Output "To execute, pass -Execute -Confirmation 'DROP gymmind'."
    return
}

if ($Confirmation -cne $confirmationPhrase) {
    throw "Refusing to reset the database. Exact confirmation required: DROP gymmind"
}

if (-not (Get-Command mysql -ErrorAction SilentlyContinue)) {
    throw "mysql CLI was not found on PATH"
}

Write-Output "Resetting only the exact database target: gymmind"
& mysql --host=$HostName --port=$Port --user=$UserName --password --batch --skip-column-names --execute=$dropSql
if ($LASTEXITCODE -ne 0) {
    throw "DROP DATABASE failed with exit code $LASTEXITCODE"
}

& mysql --host=$HostName --port=$Port --user=$UserName --password --batch --skip-column-names --execute=$createSql
if ($LASTEXITCODE -ne 0) {
    throw "CREATE DATABASE failed with exit code $LASTEXITCODE"
}

Write-Output "Database gymmind was recreated."
