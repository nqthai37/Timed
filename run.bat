@echo off
echo =========================================
echo       TIMED - ZERO UI BUILD PIPELINE
echo =========================================

echo [1/4] Starting the Android Emulator...
:: Replace the name below with your exact AVD name!
start /b "" "%LOCALAPPDATA%\Android\Sdk\emulator\emulator.exe" -avd Medium_Phone

echo [2/4] Waiting for the fake phone to wake up...
"%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe" wait-for-device

echo [3/4] Compiling the App (This uses the 1.5GB RAM limit we set!)...
set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr
call gradlew installDebug

echo [4/4] Launching Timed!
"%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe" shell monkey -p com.mobile.timed -c android.intent.category.LAUNCHER 1 >nul 2>&1

echo =========================================
echo               DONE!
echo =========================================
pause