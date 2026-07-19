@echo off

echo Compilando...
call gradlew.bat assembleDebug
if errorlevel 1 (
    echo Error al compilar.
    pause
    exit /b
)

echo Instalando APK...
"C:\Android\platform-tools\adb.exe" install -r "app\build\outputs\apk\debug\app-debug.apk"

