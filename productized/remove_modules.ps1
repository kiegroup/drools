#!/usr/bin/env pwsh
#
# This script expects a configuration file separated by semicolon, where the first part is the pom and the second the modules to be removed from that pom
# It can be invoked from project root folder using: PS> .\productized\remove_modules.ps1 .\productized\modules

# Enable strict mode for better error handling
Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

# Check if config file argument is provided
if ($args.Count -lt 1) {
    Write-Error "$($MyInvocation.MyCommand.Name): Missing arguments"
    exit 1
}

$configFile = $args[0]

# Check if config file exists
if (-not (Test-Path $configFile)) {
    Write-Error "Configuration file not found: $configFile"
    exit 1
}

# Read and process each line from the config file
Get-Content $configFile | ForEach-Object {
    $line = $_.Trim()
    
    # Skip empty lines
    if ([string]::IsNullOrWhiteSpace($line)) {
        return
    }
    
    # Split by semicolon to get pom and modules
    $parts = $line -split ';', 2
    if ($parts.Count -ne 2) {
        Write-Warning "Skipping invalid line: $line"
        return
    }
    
    $pom = $parts[0].Trim()
    $modules = $parts[1].Trim()
    
    # Check if pom file exists
    if (-not (Test-Path $pom)) {
        Write-Error "POM file not found: $pom"
        exit 1
    }
    
    # Split modules by comma
    $modulesList = $modules -split ',' | ForEach-Object { $_.Trim() }
    
    foreach ($module in $modulesList) {
        # Check if module exists in pom
        $modulePattern = "<module>$module</module>"
        $pomContent = Get-Content $pom -Raw
        
        if ($pomContent -notmatch [regex]::Escape($modulePattern)) {
            Write-Error "Could not find module $module in $pom. Exiting script..."
            exit 1
        }
        
        Write-Host "Removing module $module from $pom"
        
        # Read all lines, filter out the module line, and write back
        $lines = Get-Content $pom
        $filteredLines = $lines | Where-Object { $_ -notmatch [regex]::Escape($modulePattern) }
        $filteredLines | Set-Content $pom -Encoding UTF8
    }
}
