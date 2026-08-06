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

## WebView security (production)

Capacitor hosts the SPA under `https://localhost` (`server.androidScheme`). After Bridge init, `MainActivity.hardenWebView()`:

| Setting | Production intent |
|---------|-------------------|
| `allowFileAccess` | `false` — assets are not served via `file://` |
| `allowFileAccessFromFileURLs` / `allowUniversalAccessFromFileURLs` | `false` |
| `WebView.setWebContentsDebuggingEnabled` | **false** when app is not debuggable (release) |
| Capacitor `android.webContentsDebuggingEnabled` | default follows `ApplicationInfo.FLAG_DEBUGGABLE` |

Debug APKs may keep remote debugging for Chrome inspect. Do **not** ship release with debugging forced on.

Also: never put API keys in H5 `localStorage`; list payloads use `apiKeyMasked` only (`docs/bridge-api.md` §5).

## Frontend unit tests

```bash
cd frontend && npm test   # vitest: store + plugin mocks
```

## Appium (device required)

```bash
uv pip install -r tests/appium/requirements.txt
# Start Appium 2, install APK, then:
uv run pytest tests/appium/
```

Selectors: WebView + `data-testid` — see `docs/bridge-api.md` §4.

## Related

- Frontend notes: [`frontend/README.md`](../frontend/README.md)
- Sync script: `frontend/scripts/sync-web.mjs`
- Bridge API: [`docs/bridge-api.md`](bridge-api.md)
