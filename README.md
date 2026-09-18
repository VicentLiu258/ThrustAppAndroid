# Thrust Wealth — Android

Standalone trading risk & position sizing utility built with **Kotlin + Jetpack Compose**.

## Features

- **Lot Size Calculator** — By Risk / By Lots modes, live results, quick symbol bar
- **P/L Calculator** — Profit/loss by open & close prices
- **Market Specs** — Instrument contract specs, favorites, custom instruments
- **Trade Journal** — Local trade log with Net P/L, Win Rate, Avg R:R, Max DD
- **Calculation History** — Last 50 records, tap to restore
- **Settings** — Currency, theme (Dark/Light/Auto), CSV/JSON export, legal pages
- Fully offline — Room + DataStore, no network required

## Requirements

- Android Studio Hedgehog+ / JDK 17+
- Android SDK 35, minSdk 26

## Build & Run

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew installDebug
```

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`

## Architecture

```
app/src/main/java/com/thrustwealth/app/
  ui/          # Compose screens (MVVM + StateFlow)
  engine/      # Pure calculation functions
  data/        # Room + DataStore repositories
  navigation/  # 4-tab Navigation Compose
  util/        # CSV/JSON export helpers
```

## Compliance

Every result screen shows:  
`For calculation purposes only. Not investment advice.`
