# Build unsigned release APK. Run from anywhere; resolves repo root from this script's location.
$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
Set-Location $Root

Write-Host "Building release APK..." -ForegroundColor Cyan
& "$Root\gradlew.bat" assembleRelease

$apk = Join-Path $Root "app\build\outputs\apk\release\app-release-unsigned.apk"
if (Test-Path $apk) {
    Write-Host "Release APK: $apk" -ForegroundColor Green
} else {
    Write-Error "Expected APK not found at $apk"
}
