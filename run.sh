#!/bin/sh
# Compiles and runs the app on Mac/Linux. Run it from this folder:  sh run.sh
mkdir -p out
javac -cp "lib/sqlite-jdbc.jar" -d out src/model/*.java src/dao/*.java src/app/*.java || exit 1
java -cp "out:lib/sqlite-jdbc.jar" app.Main
