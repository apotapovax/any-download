# Install Any Download on Google Pixel

## The file

| | |
|---|---|
| **APK path (Mac)** | `~/Downloads/AnyDownload.apk` |
| **Also at** | `/Users/alexp/GIT/any-download/app/build/outputs/apk/debug/app-debug.apk` |
| **Size** | ~208 MB (includes yt-dlp + FFmpeg) |
| **Package** | `com.alexp.anydownload` |

Rebuild anytime:

```bash
cd /Users/alexp/GIT/any-download
./gradlew assembleDebug
cp app/build/outputs/apk/debug/app-debug.apk ~/Downloads/AnyDownload.apk
```

---

## Method A — USB install from Mac (recommended)

### 1. Enable Developer Options on Pixel

1. **Settings → About phone**
2. Tap **Build number** 7 times
3. Enter PIN if asked → “You are now a developer”

### 2. Enable USB debugging

1. **Settings → System → Developer options**
2. Turn on **USB debugging**
3. (Optional) **Install via USB** / **USB debugging (Security settings)** if shown

### 3. Connect phone to Mac

1. Plug in USB-C cable
2. On Pixel: tap **Allow USB debugging** → check **Always allow** → **Allow**

Verify on Mac:

```bash
source ~/.zshrc
adb devices
```

You should see something like:

```
XXXXXXXX    device
```

If it says `unauthorized`, unlock the phone and accept the prompt again.

### 4. Install

```bash
adb install -r ~/Downloads/AnyDownload.apk
```

First install takes 1–2 minutes (~208 MB).

If you get a signature conflict from an older install:

```bash
adb uninstall com.alexp.anydownload
adb install ~/Downloads/AnyDownload.apk
```

### 5. Open the app

On Pixel: app drawer → **Any Download**

Or from Mac:

```bash
adb shell am start -n com.alexp.anydownload/.MainActivity
```

---

## Method B — Copy APK to phone (no USB adb)

### 1. Transfer the file

Pick one:

- **AirDrop** (if available): send `AnyDownload.apk` to Pixel → save to **Files → Downloads**
- **Google Drive / Dropbox**: upload from Mac, download on phone
- **Email**: attach to yourself (some providers block APK — Drive is safer)

### 2. Allow unknown apps

1. Open **Files** → **Downloads** → tap `AnyDownload.apk`
2. If blocked: **Settings → Security & privacy → Install unknown apps**
3. Enable for **Files** (or Chrome, if you downloaded in browser)

### 3. Install

Tap **AnyDownload.apk** → **Install** → **Open**

---

## Method C — Wireless debugging (Android 11+, no cable)

1. Phone and Mac on same Wi‑Fi
2. **Settings → Developer options → Wireless debugging** → ON
3. Tap **Pair device with pairing code**
4. On Mac:

```bash
adb pair PHONE_IP:PAIRING_PORT
# enter pairing code when prompted

adb connect PHONE_IP:DEBUG_PORT
adb install -r ~/Downloads/AnyDownload.apk
```

(IP and ports are shown on the phone’s Wireless debugging screen.)

---

## First launch checklist

1. Open **Any Download**
2. Wait ~10–30 s for engine init + yt-dlp update
3. For **private Instagram/Facebook/X** links:
   - Tap **Connect Instagram** (or Facebook / X)
   - Sign in once in the in-app browser
4. Paste a link → **Analyze** → **Download**
5. Files save to **Files → Downloads → AnyDownload**

---

## Troubleshooting

| Problem | Fix |
|---------|-----|
| `adb: command not found` | Open new terminal or `source ~/.zshrc` |
| `no devices/emulators` | Re-plug USB, unlock phone, accept RSA prompt |
| Install blocked | Enable “Install unknown apps” for Files/Chrome |
| Not enough space | Free ~500 MB (APK + extracted libs) |
| Instagram private link fails | Use **Connect Instagram** in app (app login ≠ Instagram app login) |
| YouTube 403 | Tap **Update yt-dlp** at bottom of app |

---

## Updating later

Rebuild on Mac, then:

```bash
adb install -r ~/Downloads/AnyDownload.apk
```

The `-r` flag keeps your data (cookies, settings) when possible.
