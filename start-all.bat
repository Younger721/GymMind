@echo off
setlocal EnableExtensions
cd /d "%~dp0"

echo ======================================
echo  GymMind Full Stack Startup
echo ======================================
echo.

echo [1/3] Docker infrastructure...
call "%~dp0start-infrastructure.bat"
if errorlevel 1 (
    echo.
    echo Docker startup failed.
    pause
    exit /b 1
)

echo.
echo [2/3] Backend (Spring Boot)...
if not exist "%~dp0.env" (
    echo [ERROR] Missing .env file.
    pause
    exit /b 1
)

rem 空值不覆盖：避免 .env 里占位空行冲掉系统已配置的 DASHSCOPE_API_KEY 等
for /f "usebackq eol=# tokens=1,* delims==" %%A in ("%~dp0.env") do (
    if not "%%~B"=="" set "%%A=%%~B"
)

if not defined JAVA_HOME set "JAVA_HOME=D:\java\jdk-17.0.14"
if not defined SPRING_PROFILES_ACTIVE set "SPRING_PROFILES_ACTIVE=dev"

cd /d "%~dp0backend"
rem 子进程继承当前窗口已从 .env 加载的环境变量；百炼等其余项由 Spring DotEnv 后处理器合并根 .env
start "GymMind Backend" cmd /k "cd /d %~dp0backend && set JAVA_HOME=%JAVA_HOME% && set SPRING_PROFILES_ACTIVE=%SPRING_PROFILES_ACTIVE% && mvnw.cmd spring-boot:run"

echo   Window: GymMind Backend

echo.
echo [3/3] Frontend (Vite)...
cd /d "%~dp0frontend"
start "GymMind Frontend" cmd /k "cd /d %~dp0frontend && npm run dev"
echo   Window: GymMind Frontend

echo.
echo ======================================
echo  Startup complete
echo ======================================
echo.
echo  Frontend: http://localhost:5173/
echo  Backend:  http://localhost:8080/
echo  Swagger:  http://localhost:8080/swagger-ui/index.html
echo.
echo  Update MYSQL_PASSWORD in .env if backend cannot connect to MySQL.
echo.
pause
