# Releasing Any Download

This project ships **signed APK files** via [GitHub Releases](https://github.com/apotapovax/any-download/releases). The app checks that public endpoint and can download/install newer APKs on your phone.

Android cannot hot-reload Kotlin source code. In-app updates mean: **download a new APK → Android package installer replaces the app**. That only works if every release is signed with the **same release keystore**.

## One-time setup

### 1. Make the repo public

GitHub Releases API works without a token on public repos. In GitHub → **Settings → General → Danger Zone → Change visibility → Public**.

### 2. Create a release keystore (once)

```bash
./scripts/create-release-keystore.sh
```

This creates `release.keystore` (gitignored). **Back it up somewhere safe.** If you lose it, you cannot upgrade installed copies in place — users must uninstall and reinstall.

### 3. Add GitHub Actions secrets

In the repo: **Settings → Secrets and variables → Actions → New repository secret**

| Secret | Value |
|--------|--------|
| `ANDROID_KEYSTORE_BASE64` | `base64 -i release.keystore \| pbcopy` |
| `ANDROID_KEYSTORE_PASSWORD` | keystore password |
| `ANDROID_KEY_ALIAS` | `anydownload` (default from script) |
| `ANDROID_KEY_PASSWORD` | key password |

GitHub stores these as **encrypted repository secrets**. They are injected into the release workflow at runtime only, masked in logs, and never written to the repo. The workflow also deletes `release.keystore` after each run and refuses to fall back to unsigned APKs when secrets are missing.

### Secret safety in CI

- Secrets are **not** committed to git (`release.keystore` is gitignored).
- The workflow runs **only** on `apotapovax/any-download` (not forks).
- Gradle runs with `--quiet` to avoid verbose logs.
- Keystore file is removed in an `always()` cleanup step.
- Only the signed APK and `update-metadata.json` are uploaded to Releases — never the keystore or passwords.

### 4. Optional local release signing

Add to `local.properties` (gitignored):

```properties
RELEASE_STORE_FILE=/absolute/path/to/release.keystore
RELEASE_STORE_PASSWORD=...
RELEASE_KEY_ALIAS=anydownload
RELEASE_KEY_PASSWORD=...
```

Then:

```bash
./gradlew assembleRelease
```

APK: `app/build/outputs/apk/release/app-arm64-v8a-release.apk`

## Publishing an update

1. **Bump version** in `app/build.gradle.kts`:
   - `versionCode` — must increase every release (integer)
   - `versionName` — human-readable (e.g. `1.0.1`)

2. Commit and push to `main`.

3. Tag and push:

```bash
git tag v1.0.1
git push origin v1.0.1
```

4. GitHub Actions (`.github/workflows/release.yml`) will:
   - Build a signed release APK
   - Upload `AnyDownload-arm64-v8a.apk`
   - Upload `update-metadata.json` (used by the in-app updater)

You can also run the workflow manually from **Actions → Release APK → Run workflow** (uses `v{versionName}` as the tag name).

## In-app updates (on your phone)

1. Open the hamburger menu → **Credits**
2. The app checks GitHub Releases automatically
3. If a newer `versionCode` exists:
   - **Download update** → downloads the release APK
   - **Install update** → opens Android’s installer
4. First time: Android may ask you to allow **Install unknown apps** for Any Download

### Important notes

- Install the **first copy** from a GitHub Release (signed release), not a debug APK, so future updates can replace it.
- Debug and release builds use different signatures — switching between them requires uninstall first.
- The repo must stay **public** (or you’d need to embed a GitHub token in the app, which is not recommended).

## Release assets

| File | Purpose |
|------|---------|
| `AnyDownload-arm64-v8a.apk` | arm64 release APK (~53 MB) |
| `update-metadata.json` | `versionCode` / `versionName` for the in-app checker |

## Troubleshooting

| Problem | Fix |
|---------|-----|
| “No GitHub release found” | Publish at least one release |
| “Release is missing update-metadata.json” | Re-run workflow; both files must be attached |
| Install blocked | Settings → Apps → Any Download → Install unknown apps → Allow |
| “App not installed” on update | Different signing key — uninstall old APK, install release build |
