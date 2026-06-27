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
