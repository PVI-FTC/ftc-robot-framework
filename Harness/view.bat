@echo off
rem ---------------------------------------------------------------------------
rem Opens the harness viewer window. Double-click this file.
rem
rem   - Double-click: opens the newest recording in build\recordings
rem     (or runs the first ready-made scenario if there are none yet).
rem   - Drag a recording (.json) onto this file: opens that recording.
rem
rem It does the same as typing ".\gradlew.bat viewRecording" in PowerShell, but sets
rem JAVA_HOME to Android Studio's Java for you. Nothing outside this window changes.
rem ---------------------------------------------------------------------------
setlocal

rem Run from the folder this file is in (Harness\), wherever it was started from.
cd /d "%~dp0"

rem Use Android Studio's built-in Java unless JAVA_HOME is already set.
if not defined JAVA_HOME set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
if not exist "%JAVA_HOME%\bin\java.exe" (
    echo Could not find Java at "%JAVA_HOME%".
    echo Install Android Studio, or set JAVA_HOME to a Java 25 folder, and try again.
    goto :failed
)

echo Opening the viewer. The first time can take a minute while Gradle builds...
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
