# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is an Android application that implements an AI-powered mobile automation assistant. It uses computer vision (screen capture) and vision-language AI models to analyze the screen and automate UI interactions through Android's Accessibility Service.

## Build Commands

```bash
# Build the project
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Run unit tests
./gradlew test

# Run instrumented tests (requires connected device/emulator)
./gradlew connectedAndroidTest

# Clean build
./gradlew clean

# Lint check
./gradlew lint
```

## Architecture

The app follows a layered architecture with these key components:

### Core Automation Pipeline
1. **Screen Capture** (`screen/`): Captures screenshots via MediaProjection API through a foreground service (`ScreenCaptureService`)
2. **AI Analysis** (`api/`): Sends screenshots to vision-language AI APIs (supports multiple providers via `ApiProvider`)
3. **Action Execution** (`accessibility/`): Uses `AutoService` (AccessibilityService) to perform gestures and UI interactions

### Agent Engine (ReAct Pattern)
The `AgentEngine` class (`agent/AgentEngine.kt`) implements a Reason-Act-Observe loop:
- **Observe**: Captures current screen state
- **Reason**: Sends screen to AI with task context, receives thinking and action
- **Act**: Executes the parsed action via `AutoService`
- **Observe**: Captures result, adds to context, loops until `finish()` is called

Coordinate system: The agent works in a normalized 1080x2400 coordinate space. `CoordinateMapper` scales coordinates to actual device resolution before execution.

### Key Dependencies
- `MyApplication`: Singleton holder for `TaskEngine`, `AgentEngine`, and `ZhipuApiClient`
- `ChatViewModel`: Orchestrates agent execution, manages chat sessions via `ChatRepository`
- `TaskEngine`: Legacy orchestrator (being replaced by `AgentEngine`)

### Data Layer
- **Room Database**: Stores `SessionEntity` and `MessageEntity` for chat history
- **Repository Pattern**: `ChatRepository` mediates between ViewModels and DAOs
- **PreferencesManager**: Stores API key, provider config, and user preferences

### UI Layer (Jetpack Compose)
- `MainApp`: Navigation host with bottom bar (Chat, Settings, API Test, Logs, Profile)
- `ChatScreen`: Main interface for task automation, shows agent steps and thinking
- `PermissionScreen`: Guides users through required permissions

## Required Permissions

The app requires these runtime permissions/configurations:
1. **Accessibility Service**: User must enable in system settings
2. **Screen Capture**: Requires MediaProjection permission (user consent dialog)
3. **Overlay Permission**: `SYSTEM_ALERT_WINDOW` for potential overlay features
4. **API Configuration**: User must provide API key and select provider

## API Provider System

The app supports multiple AI providers through `ApiProvider` interface. Providers are configured in `PreferencesManager` and can be swapped at runtime. The default system prompt is in Chinese and defines available tools: `click`, `swipe`, `type`, `back`, `home`, `wait`, `finish`.

## Important Files

- `agent/AgentEngine.kt`: Core ReAct agent implementation
- `agent/AgentTools.kt`: Tool definitions and action parsing
- `accessibility/AutoService.kt`: Accessibility service for UI automation
- `screen/ScreenCaptureService.kt`: Foreground service for screen capture
- `api/ZhipuApiClient.kt`: HTTP client for vision-language AI APIs
- `engine/TaskEngine.kt`: Task orchestration with retry logic
- `engine/ActionQueue.kt`: Sequential action execution with rate limiting
