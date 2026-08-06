# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Android phone assistant (灵犀) with AI-powered device automation via accessibility services.  
**UI:** React + antd-mobile SPA in Capacitor WebView.  
**Native:** Kotlin Agent / Accessibility / Shell / Room; bridge via Capacitor custom plugins.  
**Not** Jetpack Compose for app screens (Compose removed Phase 4). Keep `FloatingWindowService`.

## Build Commands

```bash
# H5 → assets/public
cd frontend && npm run build:sync

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Run all unit tests
./gradlew testDebugUnitTest

# Run specific test class
./gradlew testDebugUnitTest --tests "*.ChatFacadeLogicTest"

# Clean build
./gradlew clean
```

## Development Scripts

### Frontend (frontend/)
```bash
cd frontend && npm run dev          # browser with plugin mocks
cd frontend && npm run build:sync   # production bundle → app assets
```

### Python Scripts (scripts/)
- `start_avd.py` / emulator helpers — see `scripts/README.md`

### Appium UI Tests (tests/appium/)
```bash
uv pip install -r tests/appium/requirements.txt
uv run pytest tests/appium/
```
Selectors: migrate to WebView + `data-testid` (see `docs/bridge-api.md` §4).

## Architecture

```
app/src/main/java/com/example/myapplication/
├── accessibility/     # AutoService, NodeParser, ActionExecutor
├── agent/             # LangChainAgentEngine, AndroidTools, ModelFactory
├── api/               # CircuitBreaker, ModelFetcher
├── bridge/            # Chat / Agent / ApiConfig / Permission / Log / Shell Facades
├── config/            # AppConfig, ModelProvider
├── data/              # Room, DataStore, repositories, mappers, DTOs
├── di/                # ServiceLocator
├── network/           # Ktor, LangChain HTTP SPI, NetworkMonitor
├── plugins/           # Capacitor Plugin implementations
├── screen/            # ScreenCapture, ScreenCaptureService
├── shell/             # ShellExecutor, ShizukuHelper
├── ui/overlay/        # FloatingWindowService only (classic View)
└── utils/             # Logger, CrashHandler

frontend/
├── src/pages/         # Chat, Logs, Profile, Permission, ApiConfig, Settings, debug pages
├── src/plugins/       # Typed Capacitor wrappers (no bare window.Capacitor)
├── src/mocks/         # Web implementations for npm run dev
└── src/types/bridge.ts
```

### Key Components

**Capacitor plugins** (`plugins/` + `frontend/src/plugins/`):  
`LingxiChat`, `LingxiAgent`, `LingxiApiConfig`, `LingxiPermission`, `LingxiLog`, `LingxiShell`, `LingxiApp`.

**Facades** (`bridge/`): UI-free entry points for plugins; own FloatingWindow stop-button wiring via `ChatFacade`.

**LangChain4j Agent** (`agent/`): multi-provider models, tools, chat memory.

**Accessibility** (`accessibility/`): gestures, node tree, input.

**Data** (`data/`): Room sessions/messages/API configs; secrets stay native.

## Testing

- JUnit 4, Truth, Mockito/MockK, Robolectric, Turbine under `app/src/test/`
- Facade logic tests: `ChatFacadeLogicTest`, `ApiConfigFacadeLogicTest`
- Appium: `tests/appium/`

## Key Documentation

- `docs/bridge-api.md` — frozen bridge DTO / plugin contract
- `docs/compose-migration-inventory.md` — DELETE/KEEP inventory (Compose UI removed)
- `docs/project-architecture.md` — architecture diagrams
- `docs/superpowers/plans/2026-08-06-ionic-h5-migration.md` — migration plan
- `docs/h5-dev-setup.md` — H5 / Capacitor dev notes

## Configuration

### Build (app/build.gradle.kts)
- compileSdk/targetSdk: 36, minSdk: 26, Java/Kotlin 17
- **No** Compose BOM / `buildFeatures.compose`
- Capacitor via `:capacitor-android` project

### Key Dependencies (gradle/libs.versions.toml)
- LangChain4j: 1.12.1
- Kotlin: 2.1.10
- AGP: 8.13.2
- Room: 2.7.0
- Ktor: 3.1.1

### Frontend
- React 19, antd-mobile 5, Capacitor 7, Vite, React Router, Zustand

## Services & Permissions

- `AutoService` — accessibility automation
- `ScreenCaptureService` — media projection
- `FloatingWindowService` — task overlay (native View)

Permissions: FOREGROUND_SERVICE, POST_NOTIFICATIONS, SYSTEM_ALERT_WINDOW, INTERNET, QUERY_ALL_PACKAGES, BIND_ACCESSIBILITY_SERVICE

## Common Tasks

### Add LLM provider
1. `ModelFactory.createChatModel()` case
2. Default config / recommended models in ModelFactory

### Add accessibility tool
1. `@Tool` method on `AndroidTools.kt` (auto-registered with AiServices)

### Add bridge method
1. Facade method → Capacitor `@PluginMethod` → TS type in `bridge.ts` + wrapper → update `docs/bridge-api.md`

### Debug network
- Chucker in debug builds; Ktor logging configured
