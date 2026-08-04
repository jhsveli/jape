@echo off
rem Builds a distributable Jape app image with jlink + jpackage.
rem
rem Produces: target\dist\Jape\  (Jape.exe launcher + app jar + trimmed runtime)
rem
rem Prerequisites:
rem   - A full JDK.  Resolved automatically: %%JAVA_HOME%% if it is a full
rem     JDK, otherwise derived from java on PATH, otherwise scanned from the
rem     common JDK install locations (.jdks, Program Files).  jlink.exe,
rem     jpackage.exe and jmods must be present.
rem   - mvn on PATH (or set MVN to a maven executable)
rem   - run from the repository root, or anywhere (script locates the root)
rem
rem Optional overrides: APP_NAME, APP_VERSION, ICON

cd /d "%~dp0.."

call :find_jdk
if errorlevel 1 goto :nojava
echo     using JDK: %JAVA_HOME%

if "%APP_NAME%"=="" set APP_NAME=Jape
if "%APP_VERSION%"=="" set APP_VERSION=1.0.0
set JAR_NAME=jape-1.0-SNAPSHOT.jar
set JAR=target\%JAR_NAME%
if "%MVN%"=="" set MVN=mvn

echo --- Building jar (%MVN% clean package)
call %MVN% -q clean package
if errorlevel 1 exit /b 1

echo --- Determining required JDK modules
set MODULES=
for /f "delims=" %%M in ('"%JAVA_HOME%\bin\jdeps" --print-module-deps --ignore-missing-deps %JAR% 2^>nul') do set MODULES=%%M
if "%MODULES%"=="" set MODULES=java.base,java.desktop,java.logging
echo     modules: %MODULES%

echo --- Building trimmed runtime (jlink)
"%JAVA_HOME%\bin\jlink" ^
    --module-path "%JAVA_HOME%\jmods" ^
    --add-modules %MODULES% ^
    --strip-debug --no-header-files --no-man-pages --compress zip-6 ^
    --output target\runtime
if errorlevel 1 exit /b 1

echo --- Packaging app image (jpackage)
if exist target\package-input rmdir /s /q target\package-input
mkdir target\package-input
copy /y %JAR% target\package-input\ >nul
"%JAVA_HOME%\bin\jpackage" ^
    --type exe ^
    --name %APP_NAME% ^
    --app-version %APP_VERSION% ^
    --input target\package-input ^
    --main-jar %JAR_NAME% ^
    --main-class jts.JapeGui ^
    --runtime-image target\runtime ^
    --dest target\dist
if errorlevel 1 exit /b 1

echo --- Done: target\dist\%APP_NAME%\
exit /b 0

:nojava
echo error: could not locate a full JDK. Set JAVA_HOME to a JDK (with 1>&2
echo bin\jlink.exe, bin\jpackage.exe and jmods), or install one in a 1>&2
echo standard location such as %%USERPROFILE%%\.jdks or Program Files. 1>&2
exit /b 1

:find_jdk
rem 1. Explicit JAVA_HOME (must be a full JDK)
if defined JAVA_HOME (
    if exist "%JAVA_HOME%\bin\jlink.exe" if exist "%JAVA_HOME%\bin\jpackage.exe" if exist "%JAVA_HOME%\jmods" exit /b 0
)

rem 2. Derive from the java on PATH
set "JAVA_HOME="
for /f "usebackq delims=" %%J in (`where java 2^>nul`) do (
    if not defined JAVA_HOME (
        for /f "tokens=1,* delims==" %%H in ('"%%J" -XshowSettings:properties -version 2^>^&1 ^| findstr /c:"java.home"') do set "JAVA_HOME=%%I"
    )
)
if defined JAVA_HOME (
    rem strip leading spaces from the java.home value
    for /f "tokens=* delims= " %%S in ("%JAVA_HOME%") do set "JAVA_HOME=%%S"
    if exist "%JAVA_HOME%\bin\jlink.exe" if exist "%JAVA_HOME%\bin\jpackage.exe" if exist "%JAVA_HOME%\jmods" exit /b 0
    set "JAVA_HOME="
)

rem 3. Scan common JDK install locations
for %%D in ("%USERPROFILE%\.jdks" "%ProgramFiles%\Java" "%ProgramFiles%\Eclipse Adoptium" "%ProgramFiles%\Microsoft" "C:\Program Files\Java" "C:\Program Files\Eclipse Adoptium" "C:\Program Files\Microsoft") do (
    if exist "%%~fD" (
        for /d %%J in ("%%~fD\*") do (
            if not defined JAVA_HOME (
                if exist "%%~fJ\bin\jlink.exe" if exist "%%~fJ\bin\jpackage.exe" if exist "%%~fJ\jmods" set "JAVA_HOME=%%~fJ"
            )
        )
    )
)
if defined JAVA_HOME exit /b 0
exit /b 1
