# 灵犀 H5（React + antd-mobile）

Phase 1 shell: Capacitor WebView UI. Native bridge via `src/plugins/*` only.

## Commands

```bash
npm ci               # or: npm install
npm run dev          # browser + LingxiApp mock
npm run build        # tsc + vite → dist/
npm run build:sync   # build + copy dist → ../app/src/main/assets/public
```

## Android build prerequisites (required order)

Android depends on **both** frontend `node_modules` and a synced web bundle. After a fresh clone:

```bash
cd frontend && npm ci && npm run build:sync
cd .. && ./gradlew :app:assembleDebug
```

1. **`frontend/node_modules/@capacitor/android`** — `settings.gradle.kts` includes `:capacitor-android` from this path. Without `npm ci`, Gradle configure fails.
2. **`app/src/main/assets/public/`** — **generated and gitignored**. Produced by `scripts/sync-web.mjs` (via `npm run build:sync`). Must exist/synced before install or the WebView has no H5 to load.

Do **not** commit `assets/public/`; always regenerate with `build:sync` after H5 changes.

Longer write-up: [`docs/h5-dev-setup.md`](../docs/h5-dev-setup.md).

## Android integration

- `capacitor.config.ts`: appId `com.example.myapplication`, appName 灵犀
- Does **not** generate a second `android/` tree; assets sync into existing `app` module
- After H5 changes: `npm run build:sync` then `./gradlew :app:assembleDebug`
