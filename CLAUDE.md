# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Android phone assistant application that provides AI-powered device automation through accessibility services. Built with Kotlin, Jetpack Compose, and LangChain4j for LLM integration.

## Build Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Run all unit tests
./gradlew testDebugUnitTest

# Run specific test class
./gradlew testDebugUnitTest --tests "*.CircuitBreakerTest"

# Clean build
./gradlew clean

# Check code style
./gradlew ktlintCheck
```

## Development Scripts

### Python Scripts (scripts/)
- `start_avd.py` - Launch Android emulator (`uv run python start_avd.py -l` to list AVDs)
- `scripts/README.md` - Full documentation for build/install/emulator scripts

### Appium UI Tests (tests/appium/)
```bash
# Install dependencies
uv pip install -r tests/appium/requirements.txt

# Run tests
uv run pytest tests/appium/
```

## Architecture

### Core Layers

```
app/src/main/java/com/example/myapplication/
├── accessibility/     # AccessibilityService automation (AutoService, NodeParser, ActionExecutor)
├── agent/             # LLM agent engine with LangChain4j integration
├── api/               # API client layer (CircuitBreaker, ModelFetcher, ZhipuApiClient)
├── config/            # App configuration and model provider settings
├── data/              # Data layer (Room DB, DataStore, repositories, mappers)
├── di/                # ServiceLocator for dependency injection
├── network/           # HTTP client (Ktor-based)
├── screen/            # Screen capture and image compression
├── shell/             # Shell command execution (Shizuku integration)
├── ui/                # Jetpack Compose UI screens and theme
└── utils/             # Utilities (Logger, CrashHandler)
```

### Key Components

**LangChain4j Agent** (`agent/langchain/`):
- `ModelFactory.kt` - Multi-provider model creation (OpenAI, Anthropic, Ollama, Zhipu AI, LocalAI)
- `LangChainAgentEngine.kt` - Core agent with AiServices, tool execution, chat memory
- `AndroidTools.kt` - Tool annotations for accessibility actions
- `RAGManager.kt` / `RAGAgent.kt` - Retrieval-augmented generation with local embeddings

**Accessibility Automation** (`accessibility/`):
- `AutoService.kt` - Main accessibility service with gesture support
- `NodeParser.kt` - UI tree parsing with proper node recycling
- `ActionExecutor.kt` - Click, scroll, swipe, input actions

**Data Layer** (`data/`):
- Room database with DAOs for API configs, sessions, messages
- DataStore for preferences
- Repository pattern with ServiceLocator DI

**Network** (`network/`):
- Ktor HTTP client with logging
- Circuit breaker pattern (`api/CircuitBreaker.kt`)
- Exponential backoff retry logic

## Testing

### Unit Tests Location
Tests use JUnit, Truth, Mockito, and MockK. See `LANGCHAIN4J_INTEGRATION_SUMMARY.md` for test examples.

### Test Tools
- JUnit 4
- Truth assertions
- Mockito/MockK for mocking
- Robolectric for Android framework
- Turbine for Flow testing

### Appium Integration Tests
- `tests/appium/test_api_config.py` - API configuration UI tests
- `tests/appium/test_chat_flow.py` - Chat flow tests
- Page objects in `conftest.py`

## Key Documentation

- `LANGCHAIN4J_MIGRATION.md` - LangChain4j integration guide and API reference
- `LANGCHAIN4J_INTEGRATION_SUMMARY.md` - Feature summary with quick start examples
- `docs/P0_ACCEPTANCE_CRITERIA.md` - P0 fix acceptance criteria and test status
- `docs/appium-mcp-installation-guide.md` - Appium MCP setup

## Configuration

### JVM Settings (gradle.properties)
- JVM args: `-Xmx1024m -Dfile.encoding=UTF-8`
- AndroidX enabled
- Kotlin code style: official

### Build Config (app/build.gradle.kts)
- compileSdk/targetSdk: 36
- minSdk: 26
- Java/Kotlin target: 17
- Compose enabled

### Key Dependencies (gradle/libs.versions.toml)
- LangChain4j: 1.12.1
- Kotlin: 2.1.10
- AGP: 8.13.2
- Compose BOM: 2025.08.00
- Room: 2.7.0
- Ktor: 3.1.1

## Services & Permissions

### Background Services
- `AutoService` - Accessibility service for UI automation
- `ScreenCaptureService` - Media projection for screenshots
- `FloatingWindowService` - Overlay window

### Required Permissions
- FOREGROUND_SERVICE, POST_NOTIFICATIONS
- SYSTEM_ALERT_WINDOW
- INTERNET
- QUERY_ALL_PACKAGES
- BIND_ACCESSIBILITY_SERVICE

## Common Tasks

### Add new LLM provider
1. Add provider case in `ModelFactory.createChatModel()`
2. Add default config in `ModelFactory.getDefaultConfig()`
3. Add recommended models in `ModelFactory.getRecommendedModels()`

### Add new accessibility tool
1. Add `@Tool` annotated method in `AndroidTools.kt`
2. Method will be auto-registered with LangChain4j AiServices

### Debug network requests
- Chucker library enabled in debug builds
- Ktor client logging configured
