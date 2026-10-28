@echo off
rem ---------------------------------------------------------------------------
rem Opens the harness viewer window. Double-click this file.
rem
rem   - Double-click: opens straight into "Drive: Team A TeleOp" (INIT, press Enter for PLAY).
rem     Pick a replay or a scenario from the dropdown at any time.
rem   - Drag a recording (.json) onto this file: opens that recording.
rem
rem It does the same as typing ".\gradlew.bat viewRecording" in PowerShell, but finds a Java
rem for you. Nothing outside this window changes.
rem
rem Java: any Java 17 or newer. This file looks in this order:
rem   1. Android Studio's built-in Java (its "jbr" folder)
rem   2. JAVA_HOME
rem   3. java on the PATH
rem ---------------------------------------------------------------------------
setlocal

rem Run from the folder this file is in (Harness\), wherever it was started from.
cd /d "%~dp0"

set "ANDROID_STUDIO_JAVA=C:\Program Files\Android\Android Studio\jbr"
set "JAVA_EXE="
if exist "%ANDROID_STUDIO_JAVA%\bin\java.exe" (
    set "JAVA_HOME=%ANDROID_STUDIO_JAVA%"
    set "JAVA_EXE=%ANDROID_STUDIO_JAVA%\bin\java.exe"
) else if defined JAVA_HOME (
    if exist "%JAVA_HOME%\bin\java.exe" set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
) else (
    rem No JAVA_HOME: gradlew.bat then uses java from the PATH, so check that one.
    for /f "delims=" %%j in ('where java 2^>nul') do if not defined JAVA_EXE set "JAVA_EXE=%%j"
)
if not defined JAVA_EXE (
    echo Could not find Java.
    if defined JAVA_HOME echo JAVA_HOME is set to "%JAVA_HOME%", but there is no bin\java.exe in it.
    echo The harness needs Java 17 or newer. Either install Android Studio, which includes one,
    echo or install a Java 17+ JDK, for example Eclipse Temurin from https://adoptium.net,
    echo and set JAVA_HOME to its folder. Then try again.
    goto :failed
)

rem Read the major version from the first line of "java -version", for example
rem    openjdk version "25.0.3"     (25)      or      java version "1.8.0_401"   (8)
set "JAVA_VERSION="
for /f "tokens=3" %%v in ('call "%JAVA_EXE%" -version 2^>^&1 ^| findstr /i "version"') do if not defined JAVA_VERSION set "JAVA_VERSION=%%~v"
set "JAVA_MAJOR="
for /f "tokens=1,2 delims=._-+" %%a in ("%JAVA_VERSION%") do (
    if "%%a"=="1" (set "JAVA_MAJOR=%%b") else (set "JAVA_MAJOR=%%a")
)
if not defined JAVA_MAJOR (
    echo Could not tell which Java version "%JAVA_EXE%" is. Trying anyway...
) else if %JAVA_MAJOR% LSS 17 (
    echo The Java at "%JAVA_EXE%" is version %JAVA_MAJOR%, but the harness needs Java 17 or newer.
    echo Install Android Studio, which includes one, or a Java 17+ JDK such as Eclipse Temurin
    echo from https://adoptium.net, and set JAVA_HOME to its folder. Then try again.
    goto :failed
)

echo Using Java %JAVA_MAJOR% from "%JAVA_EXE%".
echo Opening the viewer. The first time needs the internet and can take a few minutes while
echo Gradle downloads the libraries and builds...
if "%~1"=="" (
    call "%~dp0gradlew.bat" viewRecording
) else (
    call "%~dp0gradlew.bat" viewRecording "-Pfile=%~f1"
)
if errorlevel 1 goto :failed
exit /b 0

:failed
echo.
echo The viewer did not open. Read the messages above, then press any key to close.
pause >nul
exit /b 1
