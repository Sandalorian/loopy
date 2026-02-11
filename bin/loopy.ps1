# Loopy - Neo4j Load Generator
# Wrapper script for Windows (PowerShell)
# https://github.com/Sandalorian/loopy
# Works with PowerShell 5.1+ and PowerShell Core 7+

$ErrorActionPreference = "Stop"

# Determine script location
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path

# Determine LOOPY_HOME (allow override via environment variable)
if ($env:LOOPY_HOME) {
    $LoopyHome = $env:LOOPY_HOME
} else {
    $LoopyHome = Split-Path -Parent $ScriptDir
}

# Validate LOOPY_HOME structure
$LibDir = Join-Path $LoopyHome "lib"
if (-not (Test-Path $LibDir)) {
    Write-Host "ERROR: Invalid Loopy installation: lib\ directory not found in $LoopyHome" -ForegroundColor Red
    exit 1
}

# Find Java executable
$JavaCmd = $null

if ($env:JAVA_HOME) {
    $JavaPath = Join-Path $env:JAVA_HOME "bin\java.exe"
    if (Test-Path $JavaPath) {
        $JavaCmd = $JavaPath
    }
}

if (-not $JavaCmd) {
    $JavaCmd = Get-Command java -ErrorAction SilentlyContinue | Select-Object -ExpandProperty Source
}

if (-not $JavaCmd) {
    Write-Host "ERROR: Java not found. Please install Java 21+ and ensure it's in your PATH." -ForegroundColor Red
    Write-Host ""
    Write-Host "You can set JAVA_HOME to specify the Java installation directory."
    Write-Host ""
    Write-Host "Install Java from:"
    Write-Host "  - https://adoptium.net/"
    Write-Host "  - https://www.oracle.com/java/technologies/downloads/"
    exit 1
}

# Check Java version
try {
    $VersionOutput = & $JavaCmd -version 2>&1 | Select-Object -First 1
    if ($VersionOutput -match 'version "(\d+)') {
        $JavaVersion = [int]$Matches[1]
        if ($JavaVersion -lt 21) {
            Write-Host "ERROR: Java 21 or higher is required. Found: Java $JavaVersion" -ForegroundColor Red
            Write-Host ""
            Write-Host "Please install Java 21+ from:"
            Write-Host "  - https://adoptium.net/"
            Write-Host "  - https://www.oracle.com/java/technologies/downloads/"
            exit 1
        }
    }
} catch {
    Write-Host "WARNING: Could not determine Java version" -ForegroundColor Yellow
}

# Find Loopy JAR file
$LoopyJar = Get-ChildItem -Path $LibDir -Filter "loopy-*.jar" | Select-Object -First 1

if (-not $LoopyJar) {
    Write-Host "ERROR: Loopy JAR not found in $LibDir" -ForegroundColor Red
    Write-Host "Expected file matching: loopy-*.jar"
    exit 1
}

# Build JVM options
$JvmOpts = @()
if ($env:LOOPY_JAVA_OPTS) {
    $JvmOpts = $env:LOOPY_JAVA_OPTS -split ' '
}

# Execute Loopy
$AllArgs = $JvmOpts + @("-jar", $LoopyJar.FullName) + $args
& $JavaCmd @AllArgs
exit $LASTEXITCODE
