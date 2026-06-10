#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
APPS_DIR="$ROOT_DIR/emulator-apps"
AVD_NAME="${AVD_NAME:-AnyDownload_Pixel7}"

export JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@17}"
export ANDROID_HOME="${ANDROID_HOME:-/opt/homebrew/share/android-commandlinetools}"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"

INSTAGRAM_PKG="com.instagram.android"
TWITTER_PKG="com.twitter.android"

log() { echo "==> $*"; }

ensure_apks() {
  mkdir -p "$APPS_DIR"

  if [[ ! -d "$APPS_DIR/$INSTAGRAM_PKG" ]] || ! ls "$APPS_DIR/$INSTAGRAM_PKG"/*.apk &>/dev/null; then
    log "Downloading Instagram..."
    apkeep -a "$INSTAGRAM_PKG" -d apk-pure -o split_apk=true "$APPS_DIR"
    unzip -o "$APPS_DIR/$INSTAGRAM_PKG.xapk" -d "$APPS_DIR/$INSTAGRAM_PKG"
  fi

  if [[ ! -d "$APPS_DIR/$TWITTER_PKG" ]] || ! ls "$APPS_DIR/$TWITTER_PKG"/*.apk &>/dev/null; then
    log "Downloading X (Twitter)..."
    apkeep -a "$TWITTER_PKG" -d apk-pure -o split_apk=true "$APPS_DIR"
    unzip -o "$APPS_DIR/$TWITTER_PKG.xapk" -d "$APPS_DIR/$TWITTER_PKG"
  fi
}

wait_for_device() {
  log "Waiting for emulator..."
  adb wait-for-device
  for _ in $(seq 1 60); do
    if [[ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" == "1" ]]; then
      return 0
    fi
    sleep 5
  done
  echo "Emulator boot timeout" >&2
  exit 1
}

install_if_missing() {
  local pkg="$1"
  local dir="$2"

  if adb shell pm list packages | rg -q "$pkg"; then
    log "$pkg already installed"
    return 0
  fi

  log "Installing ${pkg}..."
  local apk_list
  apk_list=$(find "$dir" -maxdepth 1 -name '*.apk' | sort)
  if [[ -z "$apk_list" ]]; then
    echo "No APK files found in $dir" >&2
    exit 1
  fi
  # shellcheck disable=SC2046
  adb install-multiple -r -g $(echo "$apk_list" | tr '\n' ' ')
}

start_emulator_if_needed() {
  if adb devices | rg -q "emulator.*device"; then
    log "Emulator already running"
    return 0
  fi

  log "Starting ${AVD_NAME}..."
  nohup emulator -avd "$AVD_NAME" -gpu host >/tmp/anydownload-emulator.log 2>&1 &
  wait_for_device
}

main() {
  command -v apkeep >/dev/null || { echo "Install apkeep: brew install apkeep" >&2; exit 1; }
  command -v adb >/dev/null || { echo "Android SDK platform-tools not found" >&2; exit 1; }

  ensure_apks
  start_emulator_if_needed
  wait_for_device
  sleep 10

  install_if_missing "$INSTAGRAM_PKG" "$APPS_DIR/$INSTAGRAM_PKG"
  install_if_missing "$TWITTER_PKG" "$APPS_DIR/$TWITTER_PKG"

  log "Done. Instagram and X are on the emulator."
  adb shell pm list packages | rg "instagram|twitter"
}

main "$@"
