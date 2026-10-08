# Contributing

All changes go through pull requests — do not push directly to `main`.

## Workflow

1. Create a branch from `main`
2. Make your changes
3. Open a PR — CI must pass (Android Lint, Detekt, debug build, secret scan)
4. Merge the PR
5. Cut a release by bumping `versionCode` / `versionName` in `app/build.gradle.kts`, then:

```bash
git tag v1.0.x
git push origin v1.0.x
```

That triggers the release workflow and publishes a signed APK to GitHub Releases.
