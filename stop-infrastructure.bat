@echo off
setlocal EnableExtensions
cd /d "%~dp0"

echo ======================================
echo  Stop GymMind Docker Infrastructure
echo ======================================
echo.
echo Usage:
echo   stop-infrastructure.bat
echo   stop-infrastructure.bat -v
echo.

set "EXTRA="
if /I "%~1"=="-v" set "EXTRA=-v"
if /I "%~1"=="--volumes" set "EXTRA=-v"

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\docker\stop.ps1" %EXTRA%
exit /b %ERRORLEVEL%
