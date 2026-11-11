# MuPipe — Android app

Original app, built from scratch. Not a fork of NewPipe or PipePipe.
Package `app.mupipe.android`, dark only, no built-in player.

## Build (Windows / Linux)
1. Install JDK 17.
2. Set `ANDROID_HOME` to the Android SDK (API 34, build-tools 34.0.0).
3. Run `./gradlew assembleDebug` (or `gradlew.bat` on Windows).
4. The APK is at `app/build/outputs/apk/debug/app-debug.apk`. Sideload it; there is no Play Store release.

## What it does
- Discovery via the public Invidious API (no key) and YouTube channel RSS (no key).
- Subscriptions, groups, bulk import, 30-day history, playlists, multi-select.
- Downloads only for authorised content, via plain HTTPS range requests, WorkManager, max 3 retries.
- Tap a video opens details + Open on YouTube. There is no player inside.

## What it does not do
- No login, no age-restricted or Premium content, no DRM bypass, no scraping of youtube.com pages.
- No zero-ANR promise until tested on a real phone (Tecno Spark 20 Pro Plus, Android 14).

## Tests
`./gradlew testDebugUnitTest` — import parser, history pruning, download state machine, filter mapping.
