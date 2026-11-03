@echo off
rem ---------------------------------------------------------------------------
rem Opens VI Arena (the 3D replay, the Unity app) without the Unity Editor. Double-click this file.
rem
rem   - Double-click: opens the newest recording in th_src\build\recordings.
rem   - Drag a recording (.json) onto this file: opens that recording.
rem   - Live play: press Live mode in the app (it starts th_src\gradlew.bat unityBridge itself).
rem
rem The app is VI_Arena\dist\VI_Arena.exe. If it isn't built yet, this offers to build
rem it (VI_Arena\src\buildUnity.bat, which needs Unity installed). Running it needs nothing
rem installed; Live mode needs Java 17 or newer.
rem ---------------------------------------------------------------------------
setlocal
cd /d "%~dp0"

if not exist "VI_Arena\dist\VI_Arena.exe" (
    echo VI Arena isn't built yet ^(VI_Arena\dist\VI_Arena.exe^).
    choice /c YN /m "Build it now? It needs Unity installed and takes a few minutes"
    if errorlevel 2 exit /b 1
    call "%~dp0VI_Arena\src\buildUnity.bat"
    if not exist "VI_Arena\dist\VI_Arena.exe" exit /b 1
)

if "%~1"=="" (
    start "" "VI_Arena\dist\VI_Arena.exe" -recordings "%~dp0th_src\build\recordings"
) else (
    start "" "VI_Arena\dist\VI_Arena.exe" -recordings "%~dp0th_src\build\recordings" "%~f1"
)
exit /b 0
