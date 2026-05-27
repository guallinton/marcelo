@echo off
setlocal

set "TARGET=C:\Users\metas.asistenciales\Desktop\cursor"
set "APPDIR=%TARGET%\agenda-quimioterapia"
set "DIST=target\agenda-quimioterapia-dist"
set "JAR=agenda-quimioterapia-1.0.0.jar"

echo Compilando aplicacion...
call mvn -q -DskipTests package dependency:copy-dependencies -DincludeScope=runtime -DoutputDirectory=%DIST%\lib
if errorlevel 1 (
    echo Error al compilar. Verifique Java 17+ y Maven en PATH.
    exit /b 1
)

if not exist "%APPDIR%" mkdir "%APPDIR%"
if not exist "%APPDIR%\lib" mkdir "%APPDIR%\lib"

copy /Y "target\%JAR%" "%APPDIR%\%JAR%" >nul
xcopy /E /I /Y "%DIST%\lib" "%APPDIR%\lib" >nul

(
    echo @echo off
    echo cd /d "%%~dp0"
    echo java -cp "%JAR%;lib\*" com.oncologia.agenda.AppLauncher
) > "%APPDIR%\run-agenda.bat"

echo.
echo Despliegue completado en:
echo %APPDIR%
echo Ejecute run-agenda.bat para iniciar la agenda.

endlocal
