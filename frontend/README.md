# 灵犀 H5（React + antd-mobile）

Phase 1 shell: Capacitor WebView UI. Native bridge via `src/plugins/*` only.

## Commands

```bash
npm install
npm run dev          # browser + LingxiApp mock
npm run build        # tsc + vite → dist/
npm run build:sync   # build + copy dist → ../app/src/main/assets/public
```

## Android integration

- `capacitor.config.ts`: appId `com.example.myapplication`, appName 灵犀
- Does **not** generate a second `android/` tree; assets sync into existing `app` module
- After H5 changes: `npm run build:sync` then `./gradlew :app:assembleDebug`
