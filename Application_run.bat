@echo off
setlocal
title PDF Voice And Games - launcher
rem ============================================================================================
rem  PDF Voice And Games - one click start (Windows 10 / 11)
rem    1) the Games microservice  (own window, port 8095)
rem    2) the main app            (own window, port 8089)
rem    3) waits until http://localhost:8089 answers, then opens Microsoft Edge ONCE
rem  Translation works without Ollama (free online translator). Stop_Application.bat stops everything.
rem ============================================================================================
cd /d "%~dp0"

where mvn >nul 2>&1
if errorlevel 1 (
  echo Maven ^(mvn^) was not found on PATH. Install Maven or run the project from IntelliJ.
  pause
  exit /b 1
)

echo Starting the Games microservice in its own window ...
start "Games microservice (8095)" cmd /k "cd /d %~dp0games-service && mvn -q spring-boot:run"

echo Starting PDF Voice And Games ... (first start downloads libraries, please wait)
rem The batch file opens the browser itself, so the Java launcher is switched off for this run.
set "APP_AUTO_OPEN_BROWSER=false"
start "PDF Voice And Games (8089)" cmd /k "cd /d %~dp0 && mvn -q spring-boot:run"

echo Waiting for http://localhost:8089 ...
set /a TRIES=0
:wait
set /a TRIES+=1
netstat -ano | findstr /R /C:":8089 .*LISTENING" >nul 2>&1
if not errorlevel 1 goto ready
if %TRIES% GEQ 120 goto timeout
timeout /t 2 /nobreak >nul
goto wait

:timeout
echo.
echo The application did not start within 4 minutes. Look at the "PDF Voice And Games (8089)" window for the error.
pause
exit /b 1

:ready
timeout /t 2 /nobreak >nul
set "EDGE="
if exist "%ProgramFiles(x86)%\Microsoft\Edge\Application\msedge.exe" set "EDGE=%ProgramFiles(x86)%\Microsoft\Edge\Application\msedge.exe"
if exist "%ProgramFiles%\Microsoft\Edge\Application\msedge.exe" set "EDGE=%ProgramFiles%\Microsoft\Edge\Application\msedge.exe"
if exist "%LOCALAPPDATA%\Microsoft\Edge\Application\msedge.exe" set "EDGE=%LOCALAPPDATA%\Microsoft\Edge\Application\msedge.exe"
rem Own small Edge profile so Stop_Application.bat can close just this window (the folder name must keep "PdfVoiceAndGames").
set "PROFILE=%LOCALAPPDATA%\PdfVoiceAndGames\edge-profile"
echo Opening Microsoft Edge ...
if defined EDGE (
  start "" "%EDGE%" --user-data-dir="%PROFILE%" --no-first-run --no-default-browser-check --new-window http://localhost:8089
) else (
  start "" "microsoft-edge:http://localhost:8089"
)
echo.
echo Running. To stop everything and close the browser window, double-click Stop_Application.bat
timeout /t 5 >nul
endlocal
