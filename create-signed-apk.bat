@echo off
setlocal enabledelayedexpansion
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
    echo.
    echo After updating the passwords, press any key...
    pause
)

echo 4. Creating Signed Release APK...
echo.
echo DEBUG: About to call gradlew.bat...
call gradlew.bat clean assembleRelease
set BUILD_RESULT=%errorlevel%
echo DEBUG: gradlew.bat returned, errorlevel: %BUILD_RESULT%
echo Build completed with result: %BUILD_RESULT%

if %BUILD_RESULT%==0 (
    echo.
    echo ✅ Signed APK created successfully!
    echo Proceeding to APK archiving...
    echo.
    
    echo 5. Creating APK archive directory...
    if not exist "apk_archive" (
        mkdir apk_archive
        echo Created apk_archive directory
    )
    
    echo 6. Getting version...
    REM Use hardcoded version for now to avoid syntax issues
    set VERSION=1.11.0
    echo Found version: !VERSION!
    
    echo 7. Moving signed APK to archive...
    set APK_NAME=sms-forward-v!VERSION!-signed-%date:~6,4%%date:~3,2%%date:~0,2%.apk
    echo Target APK name: !APK_NAME!
    
    echo Checking for APK file...
    REM Use known APK name pattern for this project
    set APK_SOURCE=
    echo DEBUG: Checking for standard APK...
    if exist "app\build\outputs\apk\release\app-release.apk" (
        set APK_SOURCE=app\build\outputs\apk\release\app-release.apk
        echo Found standard APK: app-release.apk
    ) else (
        echo Standard APK not found, checking for SMS-Forward APK...
        if exist "app\build\outputs\apk\release\SMS-Forward-v1.11.0-12-20250627.apk" (
            set APK_SOURCE=app\build\outputs\apk\release\SMS-Forward-v1.11.0-12-20250627.apk
            echo Found project APK: SMS-Forward-v1.11.0-12-20250627.apk
        ) else (
            echo Listing all APK files in release directory:
            dir app\build\outputs\apk\release\*.apk
        )
    )
    
    if not "!APK_SOURCE!"=="" (
        echo ✅ APK file found: !APK_SOURCE!
        echo Copying to archive...
        copy "!APK_SOURCE!" "apk_archive\!APK_NAME!"
        set COPY_RESULT=!errorlevel!
        if !COPY_RESULT!==0 (
            echo ✅ APK successfully archived as: apk_archive\!APK_NAME!
            echo.
            echo Listing archive contents:
            dir apk_archive\*.apk
            echo.
            echo.
            echo ✅ APK archiving completed successfully!
            goto :end_script
        ) else (
            echo ❌ Failed to copy APK to archive (Error code: !COPY_RESULT!)
            echo Check permissions and disk space
        )
    ) else (
        echo ❌ No APK file found in release directory
        echo Checking build directory contents...
        if exist "app\build\outputs\apk\release\" (
            echo Release directory exists, listing contents:
            dir app\build\outputs\apk\release\
        ) else (
            echo Release directory does not exist - build may have failed
        )
    )
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

:end_script
echo.