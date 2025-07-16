@echo off
echo Digital Clock Application Build Script
echo ===================================

if "%1"=="clean" goto clean
if "%1"=="compile" goto compile
if "%1"=="run" goto run
if "%1"=="run-basic" goto run-basic
if "%1"=="run-analog" goto run-analog
if "%1"=="run-world" goto run-world
if "%1"=="run-advanced" goto run-advanced

echo Usage: build.bat [option]
echo Options:
echo   compile       - Compile all Java files
echo   clean         - Remove all compiled class files
echo   run           - Run the Advanced Digital Clock (default)
echo   run-basic     - Run the Basic Digital Clock
echo   run-analog    - Run the Analog Clock
echo   run-world     - Run the World Time Zones Clock
echo   run-advanced  - Run the Advanced Digital Clock
goto end

:clean
echo Cleaning build directory...
if exist "build\*.class" del /Q "build\*.class"
echo Done.
goto end

:compile
echo Compiling Java files...
if not exist "build" mkdir "build"
javac -d "build" "Src\AdvancedDigitalClock.java" "Src\AnalogClock.java" "Src\DigitalClock.java" "Src\WorldTimeZones.java"
if %ERRORLEVEL% NEQ 0 (
    echo Compilation failed.
    goto end
)
echo Compilation successful.
goto end

:run
:run-advanced
echo Running Advanced Digital Clock...
java -cp "build" AdvancedDigitalClock
goto end

:run-basic
echo Running Basic Digital Clock...
java -cp "build" DigitalClock
goto end

:run-analog
echo Running Analog Clock...
java -cp "build" AnalogClock
goto end

:run-world
echo Running World Time Zones Clock...
java -cp "build" WorldTimeZones
goto end

:end
