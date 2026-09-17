# GymMind Docker infrastructure startup (UTF-8 BOM)
$ErrorActionPreference = "Stop"
$ProjectRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
Set-Location $ProjectRoot

function Write-Step([string]$Message) {
    Write-Host ""
    Write-Host ">> $Message" -ForegroundColor Cyan
}

function Show-OkMessage([string]$Message) {
    Write-Host "   [OK] $Message" -ForegroundColor Green
}

function Write-WarnMsg([string]$Message) {
    Write-Host "   [!!] $Message" -ForegroundColor Yellow
}

function Write-ErrMsg([string]$Message) {
    Write-Host "   [ERR] $Message" -ForegroundColor Red
}

function Read-EnvMap([string]$Path) {
    $map = @{}
    Get-Content $Path -Encoding UTF8 | ForEach-Object {
        $line = $_.Trim()
        if ($line.Length -eq 0 -or $line.StartsWith("#")) { return }
        $idx = $line.IndexOf("=")
        if ($idx -le 0) { return }
        $key = $line.Substring(0, $idx).Trim().TrimStart([char]0xFEFF)
        $val = $line.Substring($idx + 1).Trim()
        $map[$key] = $val
    }
    return $map
}

function Get-EnvValue($Map, [string]$Key, [string]$Default) {
    if ($null -eq $Map) { return $Default }
    if ($Map.ContainsKey($Key) -and -not [string]::IsNullOrWhiteSpace($Map[$Key])) {
        return [string]$Map[$Key]
    }
    return $Default
}

function Test-DockerDaemon {
    & docker info 1>$null 2>$null
    return ($LASTEXITCODE -eq 0)
}

function Invoke-Compose([string[]]$ComposeArgs) {
    # docker compose 构建日志走 stderr，Windows PowerShell 下需静默处理
    $prevErrorAction = $ErrorActionPreference
    $ErrorActionPreference = "SilentlyContinue"
    & docker compose @ComposeArgs 2>&1 | Write-Host
    $code = $LASTEXITCODE
    if ($code -ne 0) {
        & docker-compose @ComposeArgs 2>&1 | Write-Host
        $code = $LASTEXITCODE
    }
    $ErrorActionPreference = $prevErrorAction
    return $code
}

function Test-ComposeRunning([int]$ExpectedCount) {
    $ids = & docker compose --env-file ".env" ps --status running -q 2>$null
    if ($null -eq $ids) { return $false }
    if ($ids -is [string]) { return 1 -ge $ExpectedCount }
    return @($ids).Count -ge $ExpectedCount
}

Write-Host "======================================" -ForegroundColor White
Write-Host " GymMind Docker Startup" -ForegroundColor White
Write-Host "======================================" -ForegroundColor White

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    Write-ErrMsg "docker CLI not found. Install Docker Desktop first."
    exit 1
}

if (-not (Test-DockerDaemon)) {
    Write-WarnMsg "Docker daemon is down. Starting Docker Desktop..."
    $desktopPaths = @(
        "$env:ProgramFiles\Docker\Docker\Docker Desktop.exe",
        "${env:ProgramFiles(x86)}\Docker\Docker\Docker Desktop.exe"
    )
    $started = $false
    foreach ($p in $desktopPaths) {
        if (Test-Path $p) {
            Start-Process -FilePath $p | Out-Null
            $started = $true
            break
        }
    }
    if (-not $started) {
        Write-ErrMsg "Docker Desktop executable not found."
        exit 1
    }
    Write-Step "Waiting for Docker Desktop (max 3 min)..."
    $deadline = (Get-Date).AddMinutes(3)
    while ((Get-Date) -lt $deadline) {
        Start-Sleep -Seconds 5
        if (Test-DockerDaemon) { break }
        Write-Host "   ... waiting" -ForegroundColor DarkGray
    }
    if (-not (Test-DockerDaemon)) {
        Write-ErrMsg "Docker daemon timeout."
        exit 1
    }
}
Show-OkMessage "Docker daemon is ready"

$envExample = Join-Path $ProjectRoot ".env.example"
$envFile = Join-Path $ProjectRoot ".env"
if (-not (Test-Path $envFile)) {
    if (-not (Test-Path $envExample)) {
        Write-ErrMsg ".env.example is missing."
        exit 1
    }
    Copy-Item $envExample $envFile
    Write-WarnMsg "Created .env from .env.example. Update MYSQL_PASSWORD before backend startup."
}

$envMap = Read-EnvMap $envFile
$redisPort = Get-EnvValue $envMap "REDIS_HOST_PORT" "6379"
$esPort = Get-EnvValue $envMap "ES_HOST_PORT" "9202"
$minioApiPort = Get-EnvValue $envMap "MINIO_API_PORT" "9000"
$minioConsolePort = Get-EnvValue $envMap "MINIO_CONSOLE_PORT" "9001"
$milvusPort = Get-EnvValue $envMap "MILVUS_PORT" "19530"
$mysqlHost = Get-EnvValue $envMap "MYSQL_HOST" "localhost"
$mysqlPortLocal = Get-EnvValue $envMap "MYSQL_PORT" "3306"
$mysqlDb = Get-EnvValue $envMap "MYSQL_DATABASE" "gymmind"
$useDockerMysql = (Get-EnvValue $envMap "USE_DOCKER_MYSQL" "0") -eq "1"

& docker compose version 1>$null 2>$null
if ($LASTEXITCODE -ne 0) {
    & docker-compose version 1>$null 2>$null
    if ($LASTEXITCODE -ne 0) {
        Write-ErrMsg "docker compose is unavailable."
        exit 1
    }
}

Write-Step "Building and starting containers..."
$composeArgs = @("--env-file", ".env", "up", "-d", "--build", "--progress", "plain")
if ($useDockerMysql) {
    $composeArgs += @("--profile", "mysql")
}
$upCode = Invoke-Compose $composeArgs
$expectedServices = if ($useDockerMysql) { 6 } else { 5 }
if ($upCode -ne 0 -and (Test-ComposeRunning $expectedServices)) {
    Write-WarnMsg "docker compose exit code was $upCode, but core containers are running."
    $upCode = 0
}
if ($upCode -ne 0) {
    Write-ErrMsg "docker compose up failed. Run: docker compose logs"
    exit 1
}
Show-OkMessage "Containers started"

function Wait-Http([string]$Name, [string]$Url, [int]$MaxSeconds) {
    Write-Host "   waiting $Name ..." -NoNewline
    $deadline = (Get-Date).AddSeconds($MaxSeconds)
    while ((Get-Date) -lt $deadline) {
        try {
            $resp = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 5
            if ($resp.StatusCode -ge 200 -and $resp.StatusCode -lt 500) {
                Write-Host " OK" -ForegroundColor Green
                return $true
            }
        } catch {
            # retry
        }
        Start-Sleep -Seconds 3
        Write-Host "." -NoNewline
    }
    Write-Host " TIMEOUT" -ForegroundColor Red
    return $false
}

function Wait-Tcp([string]$Name, [string]$HostName, [string]$Port, [int]$MaxSeconds) {
    $target = "${HostName}:${Port}"
    Write-Host "   waiting $Name ($target) ..." -NoNewline
    $deadline = (Get-Date).AddSeconds($MaxSeconds)
    while ((Get-Date) -lt $deadline) {
        try {
            $client = New-Object System.Net.Sockets.TcpClient
            $client.Connect($HostName, [int]$Port)
            $client.Close()
            Write-Host " OK" -ForegroundColor Green
            return $true
        } catch {
            # retry
        }
        Start-Sleep -Seconds 2
        Write-Host "." -NoNewline
    }
    Write-Host " TIMEOUT" -ForegroundColor Red
    return $false
}

Write-Step "Health checks..."
$allOk = $true
if (-not (Wait-Tcp "Redis" "127.0.0.1" $redisPort 90)) { $allOk = $false }
if (-not (Wait-Http "Elasticsearch" "http://127.0.0.1:${esPort}/_cluster/health" 240)) { $allOk = $false }
if (-not (Wait-Http "MinIO" "http://127.0.0.1:${minioApiPort}/minio/health/live" 120)) { $allOk = $false }
if (-not (Wait-Http "Milvus" "http://127.0.0.1:9091/healthz" 300)) { $allOk = $false }

if ($useDockerMysql) {
    $mysqlDockerPort = Get-EnvValue $envMap "MYSQL_HOST_PORT" "3307"
    if (-not (Wait-Tcp "MySQL" "127.0.0.1" $mysqlDockerPort 180)) { $allOk = $false }
}

Write-Step "Container status"
[void](Invoke-Compose @("ps"))

Write-Host ""
Write-Host "======================================" -ForegroundColor White
if ($allOk) {
    Write-Host " Infrastructure is ready" -ForegroundColor Green
} else {
    Write-Host " Some health checks failed. Run: docker compose logs" -ForegroundColor Yellow
}
Write-Host "======================================" -ForegroundColor White
Write-Host ""
Write-Host "Endpoints:"
Write-Host "  Redis:           localhost:${redisPort}"
Write-Host "  Elasticsearch:   http://localhost:${esPort}"
Write-Host "  Milvus:          localhost:${milvusPort}"
Write-Host "  MinIO API:       http://localhost:${minioApiPort}"
Write-Host "  MinIO Console:   http://localhost:${minioConsolePort}"
if ($useDockerMysql) {
    $mysqlDockerPort = Get-EnvValue $envMap "MYSQL_HOST_PORT" "3307"
    Write-Host "  MySQL (Docker):  localhost:${mysqlDockerPort}"
} else {
    Write-Host "  MySQL (local):   ${mysqlHost}:${mysqlPortLocal}/${mysqlDb}"
}
Write-Host ""
Write-Host "Backend profile: SPRING_PROFILES_ACTIVE=dev,docker"
Write-Host "One-click full stack: start-all.bat"
Write-Host ""

if (-not $allOk) { exit 2 }
exit 0
