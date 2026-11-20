@echo off
setlocal
cd /d "%~dp0"
if exist build\classes rmdir /s /q build\classes
mkdir build\classes
(for /r src %%f in (*.java) do call :writeSource "%%f") > build\sources.txt
javac --release 17 -encoding UTF-8 -d build\classes @build\sources.txt
if errorlevel 1 exit /b 1
jar --create --file build\library-system.jar --main-class main.Main -C build\classes .
if errorlevel 1 exit /b 1
echo Built build\library-system.jar
exit /b 0

:writeSource
set "librarySourcePath=%~1"
rem javac argument files treat backslashes as escapes, even inside quotes.
echo "%librarySourcePath:\=/%"
exit /b 0
