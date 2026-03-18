@echo off
REM This script expects a configuration file separated by semicolon, where the first part is the pom and the second the modules to be removed from that pom
REM It can be invoked from project root folder using: remove_modules.bat .\productized\modules

setlocal enabledelayedexpansion

if "%~1"=="" (
    echo %~nx0: Missing arguments
    exit /b 1
)

set "config_file=%~1"

if not exist "%config_file%" (
    echo Configuration file "%config_file%" not found
    exit /b 1
)

for /f "usebackq delims=" %%L in ("%config_file%") do (
    set "line=%%L"
    
    REM Split line by semicolon
    for /f "tokens=1,2 delims=;" %%A in ("!line!") do (
        set "pom=%%A"
        set "modules=%%B"
        
        REM Process each module (comma-separated)
        set "modules_list=!modules:,= !"
        
        for %%M in (!modules_list!) do (
            set "module=%%M"
            
            REM Check if module exists in pom
            findstr /C:"<module>!module!</module>" "!pom!" >nul 2>&1
            if errorlevel 1 (
                echo Could not find module !module! in !pom!. Exiting script...
                exit /b 1
            )
            
            echo Removing module !module! from !pom!
            
            REM Create temporary file and remove the module line
            powershell -Command "(Get-Content '!pom!') | Where-Object { $_ -notmatch '<module>!module!</module>' } | Set-Content '!pom!.tmp'"
            
            REM Replace original file with temporary file
            move /y "!pom!.tmp" "!pom!" >nul
        )
    )
)

echo Done.
endlocal
