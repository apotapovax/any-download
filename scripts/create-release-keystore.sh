#!/usr/bin/env bash
set -euo pipefail

KEYSTORE="${1:-release.keystore}"
ALIAS="${2:-anydownload}"

if [[ -f "$KEYSTORE" ]]; then
  echo "Keystore already exists: $KEYSTORE" >&2
  exit 1
fi

echo "Creating release keystore: $KEYSTORE (alias: $ALIAS)"
echo "Use the same passwords for both prompts if you prefer a single secret in CI."

keytool -genkeypair \
  -v \
  -keystore "$KEYSTORE" \
  -alias "$ALIAS" \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -storetype PKCS12 \
  -dname "CN=Any Download, OU=Personal, O=apotapovax, L=Local, S=Local, C=US"

echo
echo "Done. Next steps:"
echo "  1. Back up $KEYSTORE securely"
echo "  2. Add GitHub secrets (see RELEASE.md)"
echo "  3. base64 -i $KEYSTORE | pbcopy   # ANDROID_KEYSTORE_BASE64"
