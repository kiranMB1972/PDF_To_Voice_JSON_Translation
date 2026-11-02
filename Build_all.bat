@echo off
setlocal
title PDF To Voice JSON Translation - Build all
rem Builds both parts (downloads libraries on the first run): the Games microservice, then the main app.
rem Needs JDK 21 and Maven 3.9 on PATH. Add "test" below instead of -DskipTests to run the unit tests too.
cd /d "%~dp0"
where mvn >nul 2>&1
if errorlevel 1 (
  echo Maven ^(mvn^) was not found on PATH.
  pause
  exit /b 1
)
echo ===== Building the Games microservice =====
call mvn -f "%~dp0games-service\pom.xml" -U clean install -DskipTests
if errorlevel 1 (
  echo GAMES SERVICE BUILD FAILED
  pause
  exit /b 1
)
echo ===== Building the main app =====
call mvn -f "%~dp0pom.xml" -U clean install -DskipTests
if errorlevel 1 (
  echo MAIN APP BUILD FAILED
  pause
  exit /b 1
)
echo.
echo ALL BUILDS SUCCESS. Start with Application_run.bat, stop with Stop_Application.bat
pause
endlocal
