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
rem
rem Before building, it checks the drive it's on. On a USB stick (removable, or formatted FAT32
rem or exFAT) or a drive with under 1 GB free, building is slow and often fails, so it says so
rem and asks before going on.
rem ---------------------------------------------------------------------------
setlocal

rem Run from the Java harness folder (th_src\, next to this file), wherever it was started from.
cd /d "%~dp0th_src"

rem ---- Is this drive a good place to build? ---------------------------------------------
rem Windows' own PowerShell (Get-Volume) says the drive's type, file system and free space,
rem as "Removable;FAT32;290". If it can't tell (a network folder, an old Windows), carry on.
rem HARNESS_TEST_DRIVE_INFO lets a test pretend to be on another drive, in the same form.
set "DRIVE_INFO=%HARNESS_TEST_DRIVE_INFO%"
set "DRIVE_LETTER=%~d0"
set "DRIVE_LETTER=%DRIVE_LETTER:~0,1%"
if not defined DRIVE_INFO for /f "delims=" %%i in ('powershell -NoProfile -Command "try { $v = Get-Volume -DriveLetter %DRIVE_LETTER% -ErrorAction Stop; '{0};{1};{2}' -f $v.DriveType, $v.FileSystemType, [math]::Floor($v.SizeRemaining / 1MB) } catch { }" 2^>nul') do set "DRIVE_INFO=%%i"
set "DRIVE_TYPE="
set "DRIVE_FS="
set "DRIVE_FREE_MB="
if defined DRIVE_INFO for /f "tokens=1-3 delims=;" %%a in ("%DRIVE_INFO%") do (
    set "DRIVE_TYPE=%%a"
    set "DRIVE_FS=%%b"
    set "DRIVE_FREE_MB=%%c"
)
set "DRIVE_RISKY="
if /i "%DRIVE_TYPE%"=="Removable" set "DRIVE_RISKY=1"
if /i "%DRIVE_FS%"=="FAT32" set "DRIVE_RISKY=1"
if /i "%DRIVE_FS%"=="FAT" set "DRIVE_RISKY=1"
if /i "%DRIVE_FS%"=="exFAT" set "DRIVE_RISKY=1"
set "DRIVE_LOW="
rem Unknown free space counts as plenty (cmd can't compare an empty value).
if not defined DRIVE_FREE_MB set "DRIVE_FREE_MB=999999"
if %DRIVE_FREE_MB% LSS 1024 set "DRIVE_LOW=1"
if not defined DRIVE_RISKY if not defined DRIVE_LOW goto :driveOk

echo.
if /i "%DRIVE_TYPE%"=="Removable" echo This copy of the harness is on a USB stick or other removable drive (%DRIVE_FS%).
if /i not "%DRIVE_TYPE%"=="Removable" if defined DRIVE_RISKY echo This copy of the harness is on a drive formatted %DRIVE_FS%, like a USB stick.
if defined DRIVE_LOW echo The drive has only %DRIVE_FREE_MB% MB free; building needs about 1 GB.
echo Building here is slow and often fails (for example "Unable to delete directory").
echo Copy the whole ftc-robot-framework folder to this PC's own drive (for example C:\Dev)
echo and run view.bat from there.
echo.
set "ANSWER="
set /p "ANSWER=Build here anyway? Type Y and press Enter to go on, or just press Enter to stop: "
if /i not "%ANSWER%"=="Y" goto :stoppedForDrive
:driveOk

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
    call "%~dp0th_src\gradlew.bat" viewRecording
) else (
    call "%~dp0th_src\gradlew.bat" viewRecording "-Pfile=%~f1"
)
if errorlevel 1 goto :failed
exit /b 0

:failed
echo.
echo The viewer did not open. Read the messages above, then press any key to close.
pause >nul
exit /b 1

:stoppedForDrive
echo.
echo Stopped. Nothing was built. Press any key to close.
pause >nul
exit /b 1
