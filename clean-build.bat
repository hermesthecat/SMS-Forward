@echo off
echo Setting Android SDK paths...
set ANDROID_SDK_ROOT=C:\Users\%USERNAME%\AppData\Local\Android\Sdk
set ANDROID_HOME=C:\Users\%USERNAME%\AppData\Local\Android\Sdk

echo Cleaning project...
gradlew.bat clean

echo Building SMS Forward project...
gradlew.bat assembleDebug

echo.
echo Clean build completed! APK location:
echo app\build\outputs\apk\debug\app-debug.apk
pause 