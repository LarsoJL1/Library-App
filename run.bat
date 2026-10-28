@echo off
rem Compiles and runs the app on Windows. Run it from this folder:  .\run.bat
if not exist out mkdir out
javac -cp "lib\sqlite-jdbc.jar" -d out src\model\*.java src\dao\*.java src\app\*.java
if errorlevel 1 (
    echo Compile failed. See the errors above.
    exit /b 1
)
java -cp "out;lib\sqlite-jdbc.jar" app.Main
