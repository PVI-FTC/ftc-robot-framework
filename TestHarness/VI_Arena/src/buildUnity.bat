@echo off
rem ---------------------------------------------------------------------------
rem Builds VI Arena (the Unity app) as a stand-alone Windows app:
rem     VI_Arena\dist\VI_Arena.exe   (opened by TestHarness\VI_Arena.bat)
rem
rem Needs Unity 6000.3.14f1 installed with Unity Hub and signed in once (the free
rem Personal licence is enough). Close the Unity Editor first: Unity can't build a
rem project that is open. Takes a few minutes. Rebuild after changing Unity scripts.
rem To share the built app, zip it with VI_Arena\package.bat.
rem
rem Same as VI Arena -> Build Windows App in the Unity Editor's menu.
rem ---------------------------------------------------------------------------
setlocal
rem This folder (VI_Arena\src, the Unity project) and TestHarness\th_src\build for the log.
cd /d "%~dp0"
set "LOG_DIR=%~dp0..\..\th_src\build"
set "DIST=%~dp0..\dist"

rem Unity's usual install folder, unless UNITY_EXE is set.
if not defined UNITY_EXE set "UNITY_EXE=C:\Program Files\Unity\Hub\Editor\6000.3.14f1\Editor\Unity.exe"
if not exist "%UNITY_EXE%" (
    echo Could not find Unity at "%UNITY_EXE%".
    echo Install Unity 6000.3.14f1 with Unity Hub, or set UNITY_EXE to Unity.exe, and try again.
    goto :failed
)
if exist "Temp\UnityLockfile" (
    echo The Unity project is open in the Unity Editor. Close the Editor and try again,
    echo or build from the Editor's menu: VI Arena -^> Build Windows App.
    goto :failed
)

if not exist "%LOG_DIR%" mkdir "%LOG_DIR%"
echo Building VI_Arena\dist\VI_Arena.exe. This takes a few minutes...
"%UNITY_EXE%" -batchmode -projectPath "%~dp0." -executeMethod VI_Arena.Editor.BuildWindowsApp.BuildFromCommandLine -logFile "%LOG_DIR%\unity-build.log"
if errorlevel 1 (
    echo The build failed. Details are in th_src\build\unity-build.log.
    goto :failed
)
if not exist "%DIST%\VI_Arena.exe" goto :failed
echo Built VI_Arena\dist\VI_Arena.exe. Open it with VI_Arena.bat.
exit /b 0

:failed
echo.
echo Press any key to close.
pause >nul
exit /b 1
