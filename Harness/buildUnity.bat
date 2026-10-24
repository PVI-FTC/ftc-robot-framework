@echo off
rem ---------------------------------------------------------------------------
rem Builds the Unity replay as a stand-alone Windows app:
rem     unity\Build\HarnessReplay.exe   (opened by unity-view.bat)
rem
rem Needs Unity 6000.3.14f1 installed with Unity Hub and signed in once (the free
rem Personal licence is enough). Close the Unity Editor first: Unity can't build a
rem project that is open. Takes a few minutes. Rebuild after changing Unity scripts.
rem
rem Same as Harness -> Build Windows App in the Unity Editor's menu.
rem ---------------------------------------------------------------------------
setlocal
cd /d "%~dp0"

rem Unity's usual install folder, unless UNITY_EXE is set.
if not defined UNITY_EXE set "UNITY_EXE=C:\Program Files\Unity\Hub\Editor\6000.3.14f1\Editor\Unity.exe"
if not exist "%UNITY_EXE%" (
    echo Could not find Unity at "%UNITY_EXE%".
    echo Install Unity 6000.3.14f1 with Unity Hub, or set UNITY_EXE to Unity.exe, and try again.
    goto :failed
)
if exist "unity\Temp\UnityLockfile" (
    echo The Unity project is open in the Unity Editor. Close the Editor and try again,
    echo or build from the Editor's menu: Harness -^> Build Windows App.
    goto :failed
)

echo Building unity\Build\HarnessReplay.exe. This takes a few minutes...
"%UNITY_EXE%" -batchmode -projectPath "%~dp0unity" -executeMethod PviHarness.Replay.Editor.BuildWindowsApp.BuildFromCommandLine -logFile "%~dp0build\unity-build.log"
if errorlevel 1 (
    echo The build failed. Details are in build\unity-build.log.
    goto :failed
)
if not exist "unity\Build\HarnessReplay.exe" goto :failed
echo Built unity\Build\HarnessReplay.exe. Open it with unity-view.bat.
exit /b 0

:failed
echo.
echo Press any key to close.
pause >nul
exit /b 1
