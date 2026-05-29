@echo off
chcp 65001 >nul
setlocal

rem Ejecutar desde la raiz del repo O desde agenda-quimioterapia tras deploy
set "WEBDIR=%~dp0web"
if not exist "%WEBDIR%\index.html" (
  set "WEBDIR=%~dp0agenda-quimioterapia\web"
)
if not exist "%WEBDIR%\index.html" (
  echo.
  echo  No se encontro la carpeta web con index.html
  echo  Coloque este archivo en la raiz del proyecto o en Desktop\cursor\agenda-quimioterapia
  echo.
  pause
  exit /b 1
)

cd /d "%WEBDIR%"
echo.
echo  Agenda web en: %WEBDIR%
echo  Abriendo http://localhost:8080
echo  Para detener: Ctrl+C
echo.

where python >nul 2>&1
if errorlevel 1 (
  echo  Python no esta en PATH. Abriendo index.html en el navegador...
  start "" "%WEBDIR%\index.html"
  pause
  exit /b 0
)

start "" "http://localhost:8080"
python -m http.server 8080

endlocal
