$ErrorActionPreference = 'Stop'
if (-not (Test-Path .\pom.xml)) { Write-Host 'ERROR: Run this script from the StudentGig folder containing pom.xml.' -ForegroundColor Red; exit 1 }
Write-Host 'Starting StudentGig...' -ForegroundColor Cyan
& mvn clean spring-boot:run
