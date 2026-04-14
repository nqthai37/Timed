@echo off
setlocal

echo ==============================
echo TIMED - ULTIMATE SAFE SCRIPT
echo ==============================

:: ===== CONFIG =====
set AVD_NAME=Medium_Phone_2
set PACKAGE_NAME=com.mobile.timed

:: Tell Windows EXACTLY where the tools live!
set ADB="%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe"
set EMULATOR="%LOCALAPPDATA%\Android\Sdk\emulator\emulator.exe"

:: ===== CHECK ADB =====
echo.
echo [1] Checking ADB...
%ADB% devices > temp.txt

findstr /R "emulator" temp.txt > nul
if %errorlevel% neq 0 (
    echo No emulator found. Starting emulator...
    start /b "" %EMULATOR% -avd %AVD_NAME%

    echo Waiting for emulator to fully boot...
    %ADB% wait-for-device >nul 2>&1
    
    :: Smart Boot Polling (Prevents ADB Offline)
    :WAIT_LOOP
    set BOOT_STATUS=0
    for /f "delims=" %%A in ('%ADB% shell getprop sys.boot_completed 2^>nul') do set BOOT_STATUS=%%A
    if "%BOOT_STATUS%"=="1" (
        echo Emulator is fully online! Letting CPU cool...
        ping -n 6 127.0.0.1 >nul
        goto :BOOT_COMPLETE
    )
    ping -n 3 127.0.0.1 >nul
    goto :WAIT_LOOP
) else (
    echo Emulator already running.
)

:BOOT_COMPLETE
del temp.txt

:: ===== BUILD & INSTALL =====
echo.
echo [2] Building & Installing APK (Safe Mode)...

:: The laptop saver!
set GRADLE_OPTS=-Xmx1536m -XX:MaxMetaspaceSize=512m
set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr

call gradlew installDebug

if %errorlevel% neq 0 (
    echo.
    echo ==============================
    echo        BUILD FAILED! 
    echo ==============================
    pause
    exit /b
)

echo INSTALL SUCCESS!

:: ===== OPEN APP =====
echo.
echo [3] Launching App...
%ADB% shell monkey -p %PACKAGE_NAME% -c android.intent.category.LAUNCHER 1 >nul 2>&1

:: ===== LOGCAT FILTER =====
echo.
echo [4] Showing logs (filtered by TIMED)...
%ADB% logcat -c 
%ADB% logcat | findstr %PACKAGE_NAME%

pause