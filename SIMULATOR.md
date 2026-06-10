# Any Download — Local Simulator Guide

Everything is installed and configured on this Mac.

## What was installed

| Component | Location |
|-----------|----------|
| Android Studio | `/Applications/Android Studio.app` |
| Java 17 | `/opt/homebrew/opt/openjdk@17` |
| Android SDK | `/opt/homebrew/share/android-commandlinetools` |
| Virtual device | `AnyDownload_Pixel7` (Pixel 7, **Android 14 + Google Play**, arm64) |
| Preinstalled apps | **Instagram**, **X (Twitter)** — via `scripts/preinstall-emulator-apps.sh` |
| Shell env | Added to `~/.zshrc` |

## Where things are

| What | Path |
|------|------|
| Project | `/Users/alexp/GIT/any-download` |
| Debug APK | `/Users/alexp/GIT/any-download/app/build/outputs/apk/debug/app-debug.apk` |
| Emulator config | `/Users/alexp/.android/avd/AnyDownload_Pixel7.avd` |
| App on device | **Any Download** (blue icon, launcher) |

---

## How to open the simulator

### Option 1 — Android Studio (easiest)

1. Open **Android Studio** from Applications (or Spotlight: `Android Studio`)
2. **Open** project: `/Users/alexp/GIT/any-download`
3. **Tools → Device Manager**
4. Click **▶** next to **AnyDownload_Pixel7**

The virtual phone window opens on your Mac desktop.

### Option 2 — Terminal

Open a **new terminal** (so `~/.zshrc` loads), then:

```bash
emulator -avd AnyDownload_Pixel7 &
```

Wait ~30–60 seconds for the home screen.

### Preinstall Instagram + X on emulator

```bash
cd /Users/alexp/GIT/any-download
./scripts/preinstall-emulator-apps.sh
```

This script downloads APKs (via `apkeep`), starts the emulator if needed, and installs Instagram and X. Requires `brew install apkeep`.

Apps appear in the emulator app drawer. **Note:** being logged into Instagram/X on the emulator does **not** share cookies with Any Download — use **Connect Instagram** inside Any Download for downloads.

---

## Navigating the emulator (Back / Home / Recents)

The virtual Pixel was using **gesture navigation** (no visible buttons). It is now set to **3-button navigation** — you should see **Back ◁  Home ○  Recents ▣** at the bottom of the screen.

If buttons disappear again after a reset:

```bash
adb shell settings put secure navigation_mode 0
```

Or on the emulator: **Settings → System → Gestures → System navigation → 3-button navigation**

### Keyboard shortcuts (Mac — click the emulator window first)

| Action | Key |
|--------|-----|
| **Back** | `Esc` or `Delete` |
| **Home** | `Cmd + H` |
| **Recents / app switcher** | Sidebar button, or `Cmd + Tab` in emulator |
| **Menu** | `Cmd + M` |

### Emulator sidebar buttons

On the **right edge** of the emulator window there is a toolbar. Click **⋯** if it is collapsed. Use the **Back**, **Home**, and **Overview** icons — they work even when on-screen buttons don't respond.

### Gesture navigation (if you switch back)

- **Home:** swipe up from the bottom center
- **Back:** swipe in from the left or right edge
- **Recents:** swipe up from bottom and hold

---

## How to open the app on the simulator

The app should already be installed as **Any Download**.

**On the emulator:** swipe up → app drawer → tap **Any Download**

**From terminal:**

```bash
adb shell am start -n com.alexp.anydownload/.MainActivity
```

---

## Rebuild and redeploy (after code changes)

```bash
cd /Users/alexp/GIT/any-download
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.alexp.anydownload/.MainActivity
```

Or in Android Studio: select **AnyDownload_Pixel7** in the device dropdown → click **Run ▶**.

---

## Quick test flow

1. Launch emulator (see above)
2. Open **Any Download**
3. Wait for engine init (~5–15 s on first launch)
4. Paste a public video URL → **Analyze** → **Download**
5. On emulator: **Files → Downloads → AnyDownload**

**Copy a download to your Mac:**

```bash
adb pull /storage/emulated/0/Download/AnyDownload/ ~/Downloads/AnyDownload/
open ~/Downloads/AnyDownload/
```

Files live **inside the emulator/phone**, not on your Mac filesystem automatically.

**Simulate share from another app:**

```bash
adb shell am start -a android.intent.action.SEND \
  -t text/plain \
  --es android.intent.extra.TEXT "https://x.com/i/status/YOUR_POST_ID" \
  com.alexp.anydownload
```

**View logs:**

```bash
adb logcat -s AnyDownloadApp
```

---

## Troubleshooting

| Problem | Fix |
|---------|-----|
| `adb: command not found` | Open a new terminal, or run `source ~/.zshrc` |
| Emulator won't start | `adb kill-server && emulator -avd AnyDownload_Pixel7` |
| App not installed | `adb install -r app/build/outputs/apk/debug/app-debug.apk` |
| Black emulator window | Wait longer; first boot can take 1–2 min |
| Download fails on IG/FB | Normal on emulator — test on real Pixel for reliability |
| No Back/Home buttons | Run `adb shell settings put secure navigation_mode 0` or use `Esc` / sidebar buttons |

---

## Stop the emulator

Close the emulator window, or:

```bash
adb -s emulator-5554 emu kill
```
