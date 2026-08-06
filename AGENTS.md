# Repository Guidelines

## Project Structure & Module Organization
This repository is a single-module Android app with a React H5 shell.

- Native: `app/src/main/java/com/example/myapplication/` — `agent/`, `accessibility/`, `api/`, `bridge/`, `data/`, `network/`, `plugins/`, `screen/`, `shell/`, `ui/overlay/` (FloatingWindow only), `utils/`.
- H5: `frontend/` — React + antd-mobile + Capacitor; build output synced to `app/src/main/assets/public`.
- Android resources: `app/src/main/res/`; LangChain4j SPI metadata under `app/src/main/resources/`.
- Helpers: `scripts/`; Appium: `tests/appium/`.

## Build, Test, and Development Commands
- `./gradlew assembleDebug` builds a debug APK.
- `./gradlew compileDebugKotlin` checks Kotlin compilation quickly.
- `./gradlew testDebugUnitTest` runs JVM unit tests.
- `./gradlew installDebug` installs the debug build to a connected device/emulator.
- `cd frontend && npm run build:sync` builds H5 and copies into `app/src/main/assets/public`.
- `cd frontend && npm run dev` runs browser mock UI.
- `uv pip install -r tests/appium/requirements.txt` installs Appium test dependencies.
- `uv run pytest tests/appium/` runs mobile UI tests (selectors migrating to WebView + `data-testid`).

## Coding Style & Naming Conventions
- **Kotlin:** official style, 4-space indent. Facades in `bridge/`, plugins in `plugins/`, repositories with `Repository` suffix.
- **TypeScript/React:** pages under `frontend/src/pages/`, plugin wrappers under `frontend/src/plugins/`, DTOs in `frontend/src/types/bridge.ts`. Prefer antd-mobile components; do not introduce Ionic/Vue.
- **Do not** add Jetpack Compose screens or `buildFeatures.compose`. Only exception: keep `FloatingWindowService` (classic View).

## Testing Guidelines
Unit tests under `app/src/test/` use JUnit 4 with Truth, Mockito/MockK, Robolectric, Turbine. Appium under `tests/appium/` with `test_` prefix. For behavior changes run at least `testDebugUnitTest`; for UI/automation also run relevant pytest when device is available.

## Commit & Pull Request Guidelines
Short imperative summaries (e.g. `feat(ui): logs and debug pages`). One logical change per commit. PRs: problem/solution, validation steps, linked issues; note permission/service/API changes. Screenshots for H5 UI changes when useful.

## Security & Configuration Tips
Do not commit API keys, device credentials, or local machine paths. Provider secrets stay in native Room/DataStore only. Verify `AndroidManifest.xml`, accessibility services, and networking carefully before merging.

## Bridge contract
Capacitor plugin methods/events/DTOs: `docs/bridge-api.md`. Keep TS types and Kotlin Facades aligned.
