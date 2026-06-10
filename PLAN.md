# Any Download — Project Plan

Personal Android app to download videos from **X/Twitter**, **Instagram**, **Facebook**, and optionally **YouTube**. Everything runs on-device; no backend.

---

## 1. Implementation Plan

### Architecture

```
┌─────────────────────────────────────────────────────────┐
│  UI (Jetpack Compose)                                   │
│  MainScreen · URL input · Share intent · Progress       │
└──────────────────────────┬──────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────┐
│  MainViewModel (MVVM)                                   │
└──────────────────────────┬──────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────┐
│  VideoDownloadRepository                                │
└──────────────────────────┬──────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────┐
│  youtubedl-android (yt-dlp + Python + FFmpeg bundled)   │
└─────────────────────────────────────────────────────────┘
                           │
                           ▼
              Downloads/AnyDownload/ on device storage
```

### Why yt-dlp on-device?

| Approach | Pros | Cons |
|----------|------|------|
| **yt-dlp bundled (chosen)** | Works offline after init; supports 1000+ sites; no server cost | Large APK (~80–120 MB); extractor updates needed |
| Backend API | Smaller app | Hosting, privacy, maintenance |
| Platform-specific scrapers | Lightweight per site | Break constantly; high maintenance |

### Core user flows

1. **Paste URL** → Analyze → pick format → Download → file in `Downloads/AnyDownload/`
2. **Share from X/IG/FB app** → Any Download opens with URL pre-filled → Analyze
3. **Open link in browser** → Share → Any Download

### Tech stack

- Kotlin, Jetpack Compose, Material 3
- `youtubedl-android` 0.18.1 (yt-dlp + FFmpeg)
- Min SDK 26, Target SDK 35
- Saves to public `Downloads/AnyDownload/` (Android scoped storage compliant)

### Phase 2 (future, not in v1)

- Cookie import (`cookies.txt`) for logged-in Instagram/Facebook
- yt-dlp auto-update on launch
- WorkManager background queue
- Room persistence for download history
- Audio-only extraction toggle

---

## 2. Testing Plan

### Unit-level (macOS — no device)

| Test | How |
|------|-----|
| URL parsing | Extract URLs from share text, trim trailing punctuation |
| Platform detection | Map hostnames → Twitter / IG / FB / YouTube |
| Error message mapping | Login/cookie errors → friendly copy |

Run later with `./gradlew test` once unit tests are added.

### Integration (emulator or Pixel)

| # | Scenario | Expected |
|---|----------|----------|
| 1 | App cold start | Engine initializes within ~5–15 s |
| 2 | Public Twitter/X video URL | Metadata + download succeeds |
| 3 | Public Instagram reel/post | Metadata + download succeeds |
| 4 | Public Facebook video | Metadata + download succeeds |
| 5 | YouTube public video (optional) | May need updated yt-dlp; test separately |
| 6 | Share intent from Chrome | URL auto-filled, analyze runs |
| 7 | Invalid URL | Clear error snackbar |
| 8 | Private/login-required content | Error mentions cookies/login |
| 9 | Open downloaded file | Video player opens via FileProvider |
| 10 | Rotate during download | Progress continues (manual QA) |

### Regression triggers

- yt-dlp version bump
- Target SDK increase
- Platform site changes (most common failure mode)

### Test URLs

Use **your own public posts** or clearly licensed test content. Do not commit third-party URLs to the repo.

---

## 3. Roadblocks & Mitigations

| Roadblock | Impact | Mitigation |
|-----------|--------|------------|
| **Instagram/Facebook login walls** | Many links fail without session cookies | v1: public links only. v2: import `cookies.txt` from browser |
| **Platform ToS** | Legal/ethical gray area for redistribution | Personal sideload only; not for Play Store |
| **yt-dlp extractor breakage** | Sites change APIs frequently | Update yt-dlp via library API; watch yt-dlp GitHub issues |
| **YouTube PO tokens / bot checks** | YouTube downloads may fail intermittently | Mark YouTube as optional; update yt-dlp aggressively |
| **APK size (~100 MB+)** | Slow install | ABI splits for release builds; sideload arm64-only for Pixel |
| **Scoped storage (Android 10+)** | Can only write Downloads/Documents reliably | Output path fixed to `Downloads/AnyDownload/` |
| **No Google Play distribution** | Policy violation for downloaders | Sideload via `adb install` or Android Studio |
| **Emulator vs real device** | x86 emulator uses different native libs | Prefer arm64 emulator on Apple Silicon or physical Pixel |
| **Cookie/session expiry** | Downloads stop working for private content | Re-export cookies periodically |

---

## 4. Local Testing on macOS

### Prerequisites

```bash
brew install openjdk@17 android-commandlinetools
# Optional but recommended:
brew install --cask android-studio
```

Set in `~/.zshrc`:

```bash
export JAVA_HOME="/opt/homebrew/opt/openjdk@17"
export ANDROID_HOME="/opt/homebrew/share/android-commandlinetools"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$PATH"
```

Create `local.properties`:

```properties
sdk.dir=/opt/homebrew/share/android-commandlinetools
```

### Build APK

```bash
cd /Users/alexp/GIT/any-download
./gradlew assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk
```

### Option A — Android Emulator (UI testing)

1. Install Android Studio
2. Device Manager → Pixel 7 → **arm64-v8a** image
3. Run from Android Studio, or:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.alexp.anydownload/.MainActivity
```

Test share intent:

```bash
adb shell am start -a android.intent.action.SEND \
  -t text/plain \
  --es android.intent.extra.TEXT "https://x.com/i/status/..." \
  com.alexp.anydownload
```

**Note:** Some platforms block emulator IPs. Use a real Pixel for reliable IG/FB tests.

### Option B — Physical Pixel (recommended)

See section 5.

### Logs

```bash
adb logcat -s AnyDownloadApp
```

---

## 5. Initial Deployment to Google Pixel

### One-time phone setup

1. **Settings → About phone** → tap Build number 7×
2. **Developer options** → USB debugging ON
3. Connect USB → accept RSA fingerprint prompt

```bash
adb devices
```

### Install

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### First-run checklist

1. Launch Any Download — wait for engine init (~5–15 s)
2. Paste public link → Analyze → Download
3. Verify file in Files → Downloads → AnyDownload
4. Test share sheet from Chrome or social apps

### Wireless debugging (Android 11+)

Developer options → Wireless debugging → pair, then:

```bash
adb pair <ip>:<pairing-port>
adb connect <ip>:<debug-port>
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 6. What Was Built (v1)

- Kotlin + Compose app with yt-dlp via `youtubedl-android`
- Paste / share / open URL flows
- Format picker, progress UI, open downloaded file
- Output: `Downloads/AnyDownload/`
- Debug build verified: `./gradlew assembleDebug` succeeds
