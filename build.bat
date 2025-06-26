@echo off
echo Setting Android SDK paths...
set ANDROID_SDK_ROOT=C:\Users\kerem\AppData\Local\Android\Sdk
set ANDROID_HOME=C:\Users\kerem\AppData\Local\Android\Sdk

echo Building SMS Forward project...
gradlew.bat assembleDebug

echo.
echo Build completed! APK location:
echo app\build\outputs\apk\debug\app-debug.apk
pause 