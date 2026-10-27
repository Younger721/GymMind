@echo off
setlocal EnableExtensions
cd /d "%~dp0.."
call "%~dp0load-root-env.bat"
if errorlevel 1 exit /b 1

if not defined DASHSCOPE_API_KEY (
    echo [FAIL] DASHSCOPE_API_KEY is not set after loading .env
    exit /b 1
)

set "TMP_BODY=%TEMP%\gymmind-dashscope-probe.json"
curl -sS -o "%TMP_BODY%" -w "%%{http_code}" -X POST "https://dashscope.aliyuncs.com/compatible-mode/v1/embeddings" ^
  -H "Authorization: Bearer %DASHSCOPE_API_KEY%" ^
  -H "Content-Type: application/json" ^
  -d "{\"model\":\"text-embedding-v2\",\"input\":[\"ping\"]}" > "%TEMP%\gymmind-dashscope-http.txt"

set /p HTTP_CODE=<"%TEMP%\gymmind-dashscope-http.txt"
if "%HTTP_CODE%"=="200" (
    echo [OK] DashScope API Key is valid ^(HTTP 200^).
    del "%TMP_BODY%" 2>nul
    del "%TEMP%\gymmind-dashscope-http.txt" 2>nul
    exit /b 0
)

echo [FAIL] DashScope rejected the key ^(HTTP %HTTP_CODE%^). Create a new API Key in Bailian console and update root .env
findstr /I "invalid_api_key Incorrect" "%TMP_BODY%" >nul 2>&1 && echo        Reason: invalid_api_key
del "%TMP_BODY%" 2>nul
del "%TEMP%\gymmind-dashscope-http.txt" 2>nul
exit /b 1
