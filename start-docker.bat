@echo off
setlocal EnableExtensions
cd /d "%~dp0"

echo ======================================
echo  Starting Docker Desktop
echo ======================================

set "DOCKER_DESKTOP=%ProgramFiles%\Docker\Docker\Docker Desktop.exe"
if not exist "%DOCKER_DESKTOP%" if defined ProgramFiles(x86) set "DOCKER_DESKTOP=%ProgramFiles(x86)%\Docker\Docker\Docker Desktop.exe"

if not exist "%DOCKER_DESKTOP%" (
    echo [ERROR] Docker Desktop executable not found.
    echo         Please install Docker Desktop first.
    exit /b 1
)

start "Docker Desktop" "%DOCKER_DESKTOP%"
echo Docker Desktop launch requested. Waiting for Docker engine...

call "%~dp0start-infrastructure.bat" %*
set "EXIT_CODE=%ERRORLEVEL%"
endlocal & exit /b %EXIT_CODE%
