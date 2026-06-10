# Any Download

Personal Android app to download videos from **X/Twitter**, **Instagram**, **Facebook**, and optionally **YouTube**. Powered by [yt-dlp](https://github.com/yt-dlp/yt-dlp) running entirely on your device.

> For personal sideload use only — not intended for Google Play.

## Quick start

### macOS build

```bash
export JAVA_HOME="/opt/homebrew/opt/openjdk@17"
export ANDROID_HOME="/opt/homebrew/share/android-commandlinetools"

# Create once (gitignored):
echo "sdk.dir=$ANDROID_HOME" > local.properties

./gradlew assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

### Install on Pixel

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Enable **Developer options → USB debugging** on the phone first.

## Usage

1. Copy a video link from X, Instagram, Facebook, or YouTube
2. Open Any Download → **Paste** → **Analyze**
3. Choose a format (or keep “Best available”) → **Download**
4. Find files in **Downloads/AnyDownload/**

Or share a link from any app and pick **Any Download**.

## Project docs

See [PLAN.md](./PLAN.md) for:

- Full implementation architecture
- Testing matrix
- Known roadblocks (cookies, ToS, yt-dlp updates)
- macOS emulator vs Pixel testing
- Step-by-step Pixel deployment

## Stack

- Kotlin + Jetpack Compose
- [youtubedl-android](https://github.com/yausername/youtubedl-android) (yt-dlp + FFmpeg)

## Known limits (v1)

- **Private or login-required** Instagram/Facebook posts usually fail without browser cookies (planned for v2)
- **YouTube** may break when Google changes bot checks — update yt-dlp when needed
- **APK is large** (~100 MB) because Python + yt-dlp + FFmpeg are bundled

## Releases & in-app updates

Signed APKs are published to [GitHub Releases](https://github.com/apotapovax/any-download/releases). The app can check for updates under **Menu → Credits**.

See [RELEASE.md](./RELEASE.md) for keystore setup, tagging, and CI workflow.
