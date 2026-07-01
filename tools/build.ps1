# Build signed release APK for sideloading. Run from anywhere; resolves repo root from this script's location.
$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
Set-Location $Root

Write-Host "Building signed release APK..." -ForegroundColor Cyan
& "$Root\gradlew.bat" assembleRelease
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

$apk = Join-Path $Root "app\build\outputs\apk\release\app-release.apk"
if (Test-Path $apk) {
    Write-Host "Release APK: $apk" -ForegroundColor Green
} else {
    Write-Error "Expected signed APK not found at $apk. Check keystore.properties and rebuild."
}
