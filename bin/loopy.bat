@echo off
REM Loopy - Neo4j Load Generator
REM Wrapper script for Windows (CMD)
REM https://github.com/Sandalorian/loopy

setlocal enabledelayedexpansion

REM Determine script location
set "SCRIPT_DIR=%~dp0"
set "SCRIPT_DIR=%SCRIPT_DIR:~0,-1%"

REM Determine LOOPY_HOME (allow override via environment variable)
if not defined LOOPY_HOME (
    for %%i in ("%SCRIPT_DIR%\..") do set "LOOPY_HOME=%%~fi"
)

REM Validate LOOPY_HOME structure
if not exist "%LOOPY_HOME%\lib" (
    echo ERROR: Invalid Loopy installation: lib\ directory not found in %LOOPY_HOME%
    exit /b 1
)

REM Find Java executable
set "JAVA_CMD="
if defined JAVA_HOME (
    if exist "%JAVA_HOME%\bin\java.exe" (
        set "JAVA_CMD=%JAVA_HOME%\bin\java.exe"
    )
)

if not defined JAVA_CMD (
    where java >nul 2>&1
    if !errorlevel! equ 0 (
        for /f "delims=" %%i in ('where java') do (
            set "JAVA_CMD=%%i"
            goto :found_java
        )
    )
)

:found_java
if not defined JAVA_CMD (
    echo ERROR: Java not found. Please install Java 21+ and ensure it's in your PATH.
    echo.
    echo You can set JAVA_HOME to specify the Java installation directory.
    echo.
    echo Install Java from:
    echo   - https://adoptium.net/
    echo   - https://www.oracle.com/java/technologies/downloads/
    exit /b 1
)

REM Find Loopy JAR file
set "LOOPY_JAR="
for %%f in ("%LOOPY_HOME%\lib\loopy-*.jar") do (
    set "LOOPY_JAR=%%f"
    goto :found_jar
)

:found_jar
if not defined LOOPY_JAR (
    echo ERROR: Loopy JAR not found in %LOOPY_HOME%\lib\
    echo Expected file matching: loopy-*.jar
    exit /b 1
)

REM Build JVM options
set "JVM_OPTS="
if defined LOOPY_JAVA_OPTS (
    set "JVM_OPTS=%LOOPY_JAVA_OPTS%"
)

REM Execute Loopy
"%JAVA_CMD%" %JVM_OPTS% -jar "%LOOPY_JAR%" %*
exit /b %errorlevel%
