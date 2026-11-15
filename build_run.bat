@echo off
set "JAVA_HOME=C:\Users\Doxaiz\AppData\Roaming\.tlauncher\legacy\Minecraft\jre\java-runtime-delta\windows-x64\java-runtime-delta"
cd /d "C:\Users\Doxaiz\Documents\antigraviti project\Doxaiz 1.21.11"
call gradlew.bat build -x test --console=plain
