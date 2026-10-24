@echo off
rem ---------------------------------------------------------------------------
rem Opens the 3D replay (the Unity app) without the Unity Editor. Double-click this file.
rem
rem   - Double-click: opens the newest recording in build\recordings.
rem   - Drag a recording (.json) onto this file: opens that recording.
rem   - Live play: start ".\gradlew.bat unityBridge" first, then pick "Live: ..." in the app.
rem
rem The app is unity\Build\HarnessReplay.exe. If it isn't built yet, this offers to build
rem it (buildUnity.bat, which needs Unity installed). Running it needs nothing installed.
rem ---------------------------------------------------------------------------
setlocal
cd /d "%~dp0"

if not exist "unity\Build\HarnessReplay.exe" (
    echo The Unity app isn't built yet ^(unity\Build\HarnessReplay.exe^).
    choice /c YN /m "Build it now? It needs Unity installed and takes a few minutes"
    if errorlevel 2 exit /b 1
    call "%~dp0buildUnity.bat"
    if not exist "unity\Build\HarnessReplay.exe" exit /b 1
)

if "%~1"=="" (
    start "" "unity\Build\HarnessReplay.exe" -recordings "%~dp0build\recordings"
) else (
    start "" "unity\Build\HarnessReplay.exe" -recordings "%~dp0build\recordings" "%~f1"
)
exit /b 0
