@echo off
echo ========================================
echo SMS Forward - Create Signed APK
echo ========================================
echo.

echo 1. Set Android SDK Root
set ANDROID_SDK_ROOT=C:\Users\%USERNAME%\AppData\Local\Android\Sdk
set ANDROID_HOME=C:\Users\%USERNAME%\AppData\Local\Android\Sdk

echo 2. Check Keystore File
if not exist "app\keystore.jks" (
    echo ⚠️ Keystore file not found!
    echo.
    echo Run the following command to create Keystore:
    echo cd app
    echo keytool -genkey -v -keystore keystore.jks -keyalg RSA -keysize 2048 -validity 10000 -alias sms-forward
    echo cd ..
    echo.
    echo Then update the passwords in gradle.properties:
    echo - RELEASE_STORE_PASSWORD=your_keystore_password_here
    echo - RELEASE_KEY_PASSWORD=your_key_password_here
    echo.
    pause
    exit /b 1
)

echo 3. Check Gradle.properties
findstr /C:"your_keystore_password_here" gradle.properties >nul
if %errorlevel%==0 (
    echo ⚠️ Gradle.properties passwords need to be updated!
    echo.
    echo Edit the following lines:
    echo - RELEASE_STORE_PASSWORD=your_keystore_password_here
    echo - RELEASE_KEY_PASSWORD=your_key_password_here
    echo.
    echo Enter the actual keystore passwords.
    echo.
    pause
    notepad gradle.properties
    echo.
    echo After updating the passwords, press any key...
    pause
)

echo 4. Creating Signed Release APK...
echo.
gradlew.bat clean assembleRelease

if %errorlevel%==0 (
    echo.
    echo ✅ Signed
    echo app\build\outputs\apk\release\
    echo.
    explorer app\build\outputs\apk\release\
) else (
    echo.
    echo ❌ APK creation failed!
    echo.
    echo Possible reasons:
    echo - Keystore password is incorrect
    echo - Android SDK is not installed
    echo - Java is not installed
    echo.
)

echo.
pause 