@echo off
rem ---------------------------------------------------------------------------
rem Zips the built VI Arena app (everything in VI_Arena\dist) into one file to share:
rem     VI_Arena\VI_Arena-<yyyy-mm-dd>.zip
rem
rem Put the zip on Google Drive or a thumb drive yourself; this never uploads anything.
rem People without Unity unzip it anywhere and double-click VI_Arena.exe. Live mode then
rem asks where their TestHarness folder is (and needs Java 17 or newer).
rem
rem Build the app first with src\buildUnity.bat (or VI Arena -> Build Windows App in Unity).
rem The zip is ignored by git.
rem ---------------------------------------------------------------------------
setlocal
cd /d "%~dp0"

if not exist "dist\VI_Arena.exe" (
    echo There is no built app to zip: dist\VI_Arena.exe is missing.
    echo Build it first with src\buildUnity.bat.
    goto :failed
)

for /f %%D in ('powershell -NoProfile -Command "Get-Date -Format yyyy-MM-dd"') do set "TODAY=%%D"
set "ZIP=VI_Arena-%TODAY%.zip"
if exist "%ZIP%" del "%ZIP%"

echo Zipping everything in dist into %ZIP% ...
rem Windows' own tar.exe writes zip files (-a picks the format from the .zip name).
tar -a -c -f "%ZIP%" -C dist *
if errorlevel 1 (
    echo Zipping failed.
    goto :failed
)

for %%Z in ("%ZIP%") do set "BYTES=%%~zZ"
set /a MB=%BYTES% / 1048576
echo.
echo Made %~dp0%ZIP%
echo Size: %MB% MB (%BYTES% bytes)
echo Copy it to Google Drive or a thumb drive to share it.
exit /b 0

:failed
echo.
echo Press any key to close.
pause >nul
exit /b 1
