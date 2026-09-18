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

for /f "usebackq eol=# tokens=1,* delims==" %%A in ("%~dp0.env") do (
    set "%%A=%%B"
)

if not defined JAVA_HOME set "JAVA_HOME=D:\java\jdk-17.0.14"
if not defined SPRING_PROFILES_ACTIVE set "SPRING_PROFILES_ACTIVE=dev,docker"

cd /d "%~dp0backend"
start "GymMind Backend" cmd /k "cd /d %~dp0backend && set JAVA_HOME=%JAVA_HOME% && set SPRING_PROFILES_ACTIVE=%SPRING_PROFILES_ACTIVE% && set MYSQL_HOST=%MYSQL_HOST% && set MYSQL_PORT=%MYSQL_PORT% && set MYSQL_USER=%MYSQL_USER% && set MYSQL_PASSWORD=%MYSQL_PASSWORD% && set JWT_SECRET=%JWT_SECRET% && set PLATFORM_ADMIN_EMAIL=%PLATFORM_ADMIN_EMAIL% && set PLATFORM_ADMIN_PASSWORD=%PLATFORM_ADMIN_PASSWORD% && set SPRING_DATA_REDIS_HOST=%SPRING_DATA_REDIS_HOST% && set SPRING_DATA_REDIS_PORT=%SPRING_DATA_REDIS_PORT% && set GYMMIND_SEARCH_KEYWORD_BACKEND=%GYMMIND_SEARCH_KEYWORD_BACKEND% && set GYMMIND_SEARCH_VECTOR_BACKEND=%GYMMIND_SEARCH_VECTOR_BACKEND% && set GYMMIND_ES_URL=%GYMMIND_ES_URL% && set GYMMIND_MILVUS_URL=%GYMMIND_MILVUS_URL% && set GYMMIND_OBJECT_STORE=%GYMMIND_OBJECT_STORE% && set GYMMIND_OBJECT_STORE_MINIO_ENDPOINT=%GYMMIND_OBJECT_STORE_MINIO_ENDPOINT% && set GYMMIND_OBJECT_STORE_MINIO_ACCESS_KEY=%GYMMIND_OBJECT_STORE_MINIO_ACCESS_KEY% && set GYMMIND_OBJECT_STORE_MINIO_SECRET_KEY=%GYMMIND_OBJECT_STORE_MINIO_SECRET_KEY% && mvnw.cmd spring-boot:run"

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
