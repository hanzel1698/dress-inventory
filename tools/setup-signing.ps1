# Uses the shared upload keystore at ~/.android/signing/upload-keystore.jks
# Run ensure-signing.ps1 in that folder if missing.
$ErrorActionPreference = "Stop"
& "$env:USERPROFILE\.android\signing\ensure-signing.ps1"
Write-Host "Release builds use the central upload keystore (same key as GitHub Actions)." -ForegroundColor Green
