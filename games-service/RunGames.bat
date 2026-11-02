@echo off
rem Entertainment Games microservice on http://localhost:8095 (the main app's top-right drop-down uses it).
cd /d "%~dp0"
call mvn -q spring-boot:run
pause
