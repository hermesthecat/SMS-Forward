@echo off
echo Setting Android SDK paths...
set ANDROID_SDK_ROOT=C:\Users\kerem\AppData\Local\Android\Sdk
set ANDROID_HOME=C:\Users\kerem\AppData\Local\Android\Sdk

echo Building RELEASE version of SMS Forward...
gradlew.bat assembleRelease

echo.
echo Release build completed! APK location:
echo app\build\outputs\apk\release\app-release.apk
pause 