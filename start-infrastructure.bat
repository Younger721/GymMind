@echo off
setlocal EnableExtensions
cd /d "%~dp0"

echo ======================================
echo  GymMind Docker Infrastructure
echo ======================================
echo.

where powershell >nul 2>&1
if errorlevel 1 (
    echo [ERROR] PowerShell not found.
    exit /b 1
)

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\docker\start.ps1"
set EXIT_CODE=%ERRORLEVEL%

echo.
if %EXIT_CODE% equ 0 (
    echo All services are ready.
) else if %EXIT_CODE% equ 2 (
    echo Containers started, but some health checks failed.
) else (
    echo Startup failed with exit code %EXIT_CODE%.
)

exit /b %EXIT_CODE%
