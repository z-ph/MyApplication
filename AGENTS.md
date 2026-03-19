# Repository Guidelines

## Project Structure & Module Organization
This repository is a single-module Android app. Main code lives in `app/src/main/java/com/example/myapplication/`, grouped by feature and layer: `agent/`, `accessibility/`, `api/`, `data/`, `network/`, `screen/`, `shell/`, `ui/`, and `utils/`. Android resources are in `app/src/main/res/`; Java service metadata such as LangChain4j SPI files lives in `app/src/main/resources/`. Python-based device and test helpers are under `scripts/`. End-to-end Appium tests are in `tests/appium/`.

## Build, Test, and Development Commands
- `.\gradlew.bat assembleDebug` builds a debug APK.
- `.\gradlew.bat compileDebugKotlin` checks Kotlin compilation quickly.
- `.\gradlew.bat testDebugUnitTest` runs JVM unit tests.
- `.\gradlew.bat installDebug` installs the debug build to a connected device/emulator.
- `uv pip install -r tests/appium/requirements.txt` installs Appium test dependencies.
- `uv run pytest tests/appium/` runs mobile UI tests.

Use PowerShell from the repository root on Windows. Prefer targeted Gradle tasks while iterating.

## Coding Style & Naming Conventions
Follow Kotlin official style with 4-space indentation and idiomatic Kotlin/Compose patterns. Use `UpperCamelCase` for classes, `lowerCamelCase` for functions and properties, and keep package names lowercase. Name screens with a `Screen` suffix, view models with `ViewModel`, repositories with `Repository`, and tests after the subject, for example `CircuitBreakerTest`. Keep new files under the existing package structure instead of creating parallel top-level folders.

## Testing Guidelines
Unit tests should live under `app/src/test/` and use JUnit 4 with Truth, Mockito/MockK, Robolectric, and Turbine where appropriate. Appium coverage belongs in `tests/appium/` with file names starting with `test_`. For behavior changes, run at least `testDebugUnitTest`; for UI or device automation changes, also run the relevant `pytest` suite.

## Commit & Pull Request Guidelines
Recent commits use short, imperative summaries such as `Fix build_apk.py Windows platform compatibility`. Keep one logical change per commit. Pull requests should include a brief problem/solution summary, validation steps run, linked issues if any, and screenshots or recordings for Compose/UI changes. Call out changes to permissions, background services, or API/provider configuration explicitly.

## Security & Configuration Tips
Do not commit API keys, device credentials, or local machine paths. Keep provider secrets in local app configuration only, and verify changes to `AndroidManifest.xml`, accessibility services, and networking code carefully before merging.
