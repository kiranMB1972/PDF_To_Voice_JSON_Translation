@echo off
setlocal
title Stop - PDF Voice And Games
rem ============================================================================================
rem  Stops PDF Voice And Games completely:
rem    1) closes the Edge window opened by this app (only that one - your other Edge windows stay open)
rem    2) stops the main app (port 8089) and the Games microservice (port 8095)
rem    3) closes their console windows
rem ============================================================================================
echo Closing the PDF Voice And Games browser window ...
powershell -NoProfile -ExecutionPolicy Bypass -Command "Get-CimInstance Win32_Process | Where-Object { $_.Name -eq 'msedge.exe' -and $_.CommandLine -like '*PdfVoiceAndGames*' } | ForEach-Object { Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue }"

echo Stopping the application ^(ports 8089 and 8095^) ...
for %%P in (8089 8095) do (
  for /f "tokens=5" %%A in ('netstat -ano ^| findstr /R /C:":%%P .*LISTENING"') do (
    taskkill /F /T /PID %%A >nul 2>&1
  )
)

echo Closing the console windows ...
taskkill /F /T /FI "WINDOWTITLE eq PDF Voice And Games (8089)*" >nul 2>&1
taskkill /F /T /FI "WINDOWTITLE eq Games microservice (8095)*" >nul 2>&1

echo.
echo Done. PDF Voice And Games is stopped and its browser window is closed.
timeout /t 3 >nul
endlocal
