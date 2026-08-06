# H5 + Capacitor Android build setup

The APK hosts a Capacitor WebView. The Android Gradle project depends on frontend npm install and a web asset sync **before** `./gradlew` can succeed.

## Required build order

```bash
# 1) Install frontend deps + build H5 + sync into Android assets
cd frontend
npm ci
npm run build:sync

# 2) Build the Android app (from repo root)
cd ..
./gradlew :app:assembleDebug
```

`npm run build:sync` = `npm run build` then `node scripts/sync-web.mjs` (copies `frontend/dist/` → `app/src/main/assets/public/` and writes `capacitor.config.json` / `capacitor.plugins.json` under `app/src/main/assets/`).

## Why this order matters

### 1. Gradle includes Capacitor from `node_modules`

`settings.gradle.kts` wires the Capacitor Android library as a composite project:

```kotlin
include(":capacitor-android")
project(":capacitor-android").projectDir =
    file("frontend/node_modules/@capacitor/android/capacitor")
```

If `frontend/node_modules` is missing (`npm ci` not run), Gradle configuration fails because that path does not exist.

### 2. Web assets are generated and gitignored

`app/src/main/assets/public/` is **not** committed (see root `.gitignore`). It is produced only by sync:

- Missing `public/` → install/run may ship an empty WebView or fail asset packaging expectations.
- Always run `npm run build:sync` after clone and after H5 changes before `installDebug` / device smoke.

`capacitor.config.json` and `capacitor.plugins.json` next to `public/` are written by the same sync script and are committed as runtime templates; re-running sync refreshes them.

## Day-to-day

| Goal | Command |
|------|---------|
| Browser UI + mock bridge | `cd frontend && npm run dev` |
| H5 only build | `cd frontend && npm run build` |
| H5 → Android assets | `cd frontend && npm run build:sync` |
| Debug APK | above sync, then `./gradlew :app:assembleDebug` |
| Install on device | above sync, then `./gradlew :app:installDebug` |

## Related

- Frontend notes: [`frontend/README.md`](../frontend/README.md)
- Sync script: `frontend/scripts/sync-web.mjs`
- Bridge API: [`docs/bridge-api.md`](bridge-api.md)
