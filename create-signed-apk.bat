@echo off
echo ========================================
echo SMS Forward - İmzalı APK Oluşturucu
echo ========================================
echo.

echo 1. Android SDK yollarını ayarlıyorum...
set ANDROID_SDK_ROOT=C:\Users\%USERNAME%\AppData\Local\Android\Sdk
set ANDROID_HOME=C:\Users\%USERNAME%\AppData\Local\Android\Sdk

echo 2. Keystore dosyası kontrolü...
if not exist "app\keystore.jks" (
    echo ⚠️ Keystore dosyası bulunamadı!
    echo.
    echo Keystore oluşturmak için şu komutu çalıştırın:
    echo cd app
    echo keytool -genkey -v -keystore keystore.jks -keyalg RSA -keysize 2048 -validity 10000 -alias sms-forward
    echo cd ..
    echo.
    echo Ardından gradle.properties dosyasındaki şifreleri güncelleyin:
    echo - RELEASE_STORE_PASSWORD=your_keystore_password_here
    echo - RELEASE_KEY_PASSWORD=your_key_password_here
    echo.
    pause
    exit /b 1
)

echo 3. Gradle.properties kontrolü...
findstr /C:"your_keystore_password_here" gradle.properties >nul
if %errorlevel%==0 (
    echo ⚠️ Gradle.properties dosyasındaki şifreleri güncellemeniz gerekiyor!
    echo.
    echo Şu satırları düzenleyin:
    echo - RELEASE_STORE_PASSWORD=your_keystore_password_here
    echo - RELEASE_KEY_PASSWORD=your_key_password_here
    echo.
    echo Gerçek keystore şifrelerinizi girin.
    echo.
    pause
    notepad gradle.properties
    echo.
    echo Şifreleri güncelledikten sonra herhangi bir tuşa basın...
    pause
)

echo 4. İmzalı Release APK oluşturuluyor...
echo.
gradlew.bat clean assembleRelease

if %errorlevel%==0 (
    echo.
    echo ✅ İmzalı APK başarıyla oluşturuldu!
    echo.
    echo 📍 APK konumu:
    echo app\build\outputs\apk\release\
    echo.
    explorer app\build\outputs\apk\release\
) else (
    echo.
    echo ❌ APK oluşturulamadı!
    echo.
    echo Olası nedenler:
    echo - Keystore şifresi yanlış
    echo - Android SDK kurulu değil
    echo - Java kurulu değil
    echo.
)

echo.
pause 