# Dress Inventory

Android wardrobe app for cataloging tops and bottoms, logging what you wear, and getting color-matched outfit suggestions.

## Features

- **Wardrobe** — Add, edit, and organize tops and bottoms with photos, colors, patterns, and types
- **Log** — Record outfits worn by date on a calendar
- **Ideas** — Daily outfit suggestions ranked by color harmony and wear rotation
- **Pairs** — Browse top/bottom combinations with match scores
- **Shop** — Wishlist gaps in your wardrobe with a curated natural-light color chart

Color matching uses HSL-based scoring with neutral detection, complementary/analogous rules, and pattern awareness (solid vs patterned).

## Requirements

- Android Studio Ladybug or newer (or JDK 17 + Android SDK)
- Android SDK with `compileSdk` 36
- Minimum device: Android 8.0 (API 26)

## Run from source

1. Clone the repository.
2. Create `local.properties` with your SDK path (Android Studio usually generates this):

   ```properties
   sdk.dir=C\:\\Path\\To\\Android\\Sdk
   ```

3. Open the project in Android Studio and run on a device/emulator, or from the repo root:

   ```powershell
   .\gradlew.bat installDebug
   ```

## Build

Debug APK:

```powershell
.\gradlew.bat assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`

Release APK (unsigned, suitable for sideloading):

```powershell
.\tools\build.ps1
```

Output: `app/build/outputs/apk/release/app-release-unsigned.apk`

## Publish / install release

Build the release APK with `tools/build.ps1`, then install via ADB:

```powershell
adb install app/build/outputs/apk/release/app-release-unsigned.apk
```

Pre-built APKs are attached to [GitHub Releases](https://github.com/hanzel1698/dress-inventory/releases).

## Publish to Google Play (internal testing)

This repo ships signed AABs to the **internal** track via GitHub Actions. Package name: `com.hanzel.dressinventory`.

### One-time Play Console setup

1. Create the app in [Google Play Console](https://play.google.com/console) with package `com.hanzel.dressinventory` (must match `applicationId` in `app/build.gradle.kts`).
2. Under **Setup → API access**, link a Google Cloud project and create a service account with **Release to production, exclude devices, and use Play App Signing** (or equivalent release permissions). Download the JSON key.
3. Add GitHub repository secrets (Settings → Secrets and variables → Actions):
   - `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON` — full service account JSON
   - `ANDROID_KEYSTORE_BASE64` — base64-encoded upload keystore (`.jks`)
   - `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`
   - `ANDROID_UPLOAD_CERT_SHA1` — SHA-1 of the upload certificate (must match Play Console → App signing → Upload key certificate)
4. Complete required Play Console forms: **App content** (privacy policy URL, data safety, ads, target audience, content rating). This app stores data only on-device and does not use the network.

   **Privacy policy URL** (after enabling GitHub Pages on the `main` branch `/docs` folder):

   ```
   https://hanzel1698.github.io/dress-inventory/privacy-policy.html
   ```

### Publish a new build

1. **Bump versionCode and Build for Play** — Actions → run `play-version-bump-build.yml`. This increments `versionCode`, commits the bump, builds a signed AAB, and attaches it to a `play-v*` GitHub Release.
2. **Upload latest AAB to Google Play** — Actions → run `upload-aab-to-play.yml`. This uploads the latest Play AAB artifact to the **internal** track with release notes from the latest commit message.

### Add internal testers

In Play Console: **Testing → Internal testing → Testers**. Add email addresses or a Google Group, then share the opt-in link with testers. They install from the Play Store link (not a direct APK).

## Project structure

```
dress-inventory/
├── app/src/main/java/com/hanzel/dressinventory/
│   ├── MainActivity.kt          # Navigation shell (5 tabs)
│   ├── AppViewModel.kt          # UI state bridge
│   ├── data/
│   │   ├── Model.kt             # Dress, AppData, color chart
│   │   ├── Matching.kt          # Outfit scoring & suggestions
│   │   └── Repository.kt        # JSON + photo persistence
│   └── ui/                      # Compose screens & theme
├── app/build.gradle.kts
├── build.gradle.kts
├── settings.gradle.kts
└── tools/build.ps1              # Release build script
```

## Data & privacy

All data is stored locally on the device:

- Wardrobe JSON: `filesDir/closet.json`
- Photos: `filesDir/photos/`

No network access; nothing leaves the device.

## Domain notes

- **Categories:** `TOP`, `BOTTOM`
- **Patterns:** `SOLID`, `PATTERNED`
- **Match score:** 0–100 from hue distance, lightness contrast, neutral handling, and pattern rules
- **Wear rotation:** Suggestions prefer items not worn recently
