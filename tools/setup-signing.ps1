# Create a local release keystore for signed sideload APKs (gitignored).
$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
$KeystoreDir = Join-Path $Root "keystore"
$KeystoreFile = Join-Path $KeystoreDir "release.jks"
$PropsFile = Join-Path $Root "keystore.properties"

if ((Test-Path $KeystoreFile) -and (Test-Path $PropsFile)) {
    Write-Host "Signing keystore already exists." -ForegroundColor Green
    exit 0
}

New-Item -ItemType Directory -Force -Path $KeystoreDir | Out-Null

$keytoolCandidates = @(
    "C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe",
    "C:\Program Files\Java\jdk-20\bin\keytool.exe"
)
if ($env:JAVA_HOME) {
    $keytoolCandidates = @(Join-Path $env:JAVA_HOME "bin\keytool.exe") + $keytoolCandidates
}
$keytool = $keytoolCandidates | Where-Object { Test-Path $_ } | Select-Object -First 1

if (-not $keytool) {
    Write-Error "keytool not found. Install JDK 17+ or Android Studio, or add keytool to PATH."
}

$password = -join ((48..57) + (65..90) + (97..122) | Get-Random -Count 24 | ForEach-Object { [char]$_ })
$alias = "dressinventory"

Write-Host "Creating release keystore at $KeystoreFile" -ForegroundColor Cyan
& $keytool -genkeypair -v `
    -storetype PKCS12 `
    -keystore $KeystoreFile `
    -alias $alias `
    -keyalg RSA `
    -keysize 2048 `
    -validity 10000 `
    -storepass $password `
    -keypass $password `
    -dname "CN=Dress Inventory, OU=Mobile, O=Hanzel, C=US"

$storeFileRelative = "keystore/release.jks" -replace '\\', '/'
@"
storeFile=$storeFileRelative
storePassword=$password
keyAlias=$alias
keyPassword=$password
"@ | Out-File -FilePath $PropsFile -Encoding ascii -NoNewline
Add-Content -Path $PropsFile -Value "" -Encoding ascii

Write-Host "Wrote $PropsFile" -ForegroundColor Green
Write-Host "Back up keystore/ and keystore.properties. You need the same key for future updates." -ForegroundColor Yellow
