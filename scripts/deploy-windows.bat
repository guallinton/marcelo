@echo off
setlocal

set "TARGET=C:\Users\metas.asistenciales\Desktop\cursor"
set "APPDIR=%TARGET%\agenda-quimioterapia"
set "DIST=target\agenda-quimioterapia-dist"
set "JAR=agenda-quimioterapia-1.3.0.jar"
set "MVN_CMD=mvn"

if exist "mvnw.cmd" (
    set "MVN_CMD=mvnw.cmd"
)

echo Compilando aplicacion...
call %MVN_CMD% -q -DskipTests package dependency:copy-dependencies -DincludeScope=runtime -DoutputDirectory=%DIST%\lib
if errorlevel 1 (
    echo Error al compilar. Verifique Java 17+ en PATH.
    exit /b 1
)

if not exist "%APPDIR%" mkdir "%APPDIR%"
if not exist "%APPDIR%\lib" mkdir "%APPDIR%\lib"

copy /Y "target\%JAR%" "%APPDIR%\%JAR%" >nul
xcopy /E /I /Y "%DIST%\lib" "%APPDIR%\lib" >nul
if exist "%DIST%\AgendaQuimioterapia.exe" (
    copy /Y "%DIST%\AgendaQuimioterapia.exe" "%APPDIR%\AgendaQuimioterapia.exe" >nul
)

(
    echo @echo off
    echo cd /d "%%~dp0"
    echo java -cp "%JAR%;lib\*" com.oncologia.agenda.AppLauncher
) > "%APPDIR%\run-agenda.bat"

echo.
echo Despliegue completado en:
echo %APPDIR%
if exist "%APPDIR%\AgendaQuimioterapia.exe" (
    echo Ejecute AgendaQuimioterapia.exe para iniciar la agenda.
) else (
    echo Ejecute run-agenda.bat para iniciar la agenda.
)

endlocal
