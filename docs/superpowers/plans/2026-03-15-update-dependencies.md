# Update Dependencies Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Update outdated project dependencies to latest stable versions and verify the build still works correctly.

**Architecture:** Update versions in `libs.versions.toml`, fix plugin configuration in `app/build.gradle.kts`, then run Gradle build to verify everything compiles and tests pass.

**Tech Stack:** Android, Kotlin, Gradle, Room, Ktor, Kotlinx Serialization, DataStore

---

## Chunk 1: Update libs.versions.toml

### Task 1: Update Room to latest version

**Files:**
- Modify: `gradle/libs.versions.toml:11`

- [ ] **Step 1: Update Room version**

Current line 11:
```toml
room = "2.6.1"
```

Change to:
```toml
room = "2.7.0"
```

- [ ] **Step 2: Verify the file was updated correctly**

Run: `cat gradle/libs.versions.toml | grep "room = "`
Expected: `room = "2.7.0"`

---

### Task 2: Update Ktor to latest version

**Files:**
- Modify: `gradle/libs.versions.toml:25`

- [ ] **Step 1: Update Ktor version**

Current line 25:
```toml
ktor = "3.0.0"
```

Change to:
```toml
ktor = "3.1.1"
```

- [ ] **Step 2: Verify the file was updated correctly**

Run: `cat gradle/libs.versions.toml | grep "ktor = "`
Expected: `ktor = "3.1.1"`

---

### Task 3: Update Kotlinx Serialization

**Files:**
- Modify: `gradle/libs.versions.toml:23`

- [ ] **Step 1: Update kotlinxSerialization version**

Current line 23:
```toml
kotlinxSerialization = "1.6.3"
```

Change to:
```toml
kotlinxSerialization = "1.7.3"
```

- [ ] **Step 2: Verify the file was updated correctly**

Run: `cat gradle/libs.versions.toml | grep "kotlinxSerialization = "`
Expected: `kotlinxSerialization = "1.7.3"`

---

### Task 4: Update DataStore

**Files:**
- Modify: `gradle/libs.versions.toml:22`

- [ ] **Step 1: Update datastore version**

Current line 22:
```toml
datastore = "1.1.1"
```

Change to:
```toml
datastore = "1.2.0"
```

- [ ] **Step 2: Verify the file was updated correctly**

Run: `cat gradle/libs.versions.toml | grep "datastore = "`
Expected: `datastore = "1.2.0"`

---

### Task 5: Update Desugar JDK

**Files:**
- Modify: `gradle/libs.versions.toml:21`

- [ ] **Step 1: Update desugarJdk version**

Current line 21:
```toml
desugarJdk = "2.1.4"
```

Change to:
```toml
desugarJdk = "2.2.0"
```

- [ ] **Step 2: Verify the file was updated correctly**

Run: `cat gradle/libs.versions.toml | grep "desugarJdk = "`
Expected: `desugarJdk = "2.2.0"`

---

### Task 6: Add kotlin-serialization plugin to version catalog

**Files:**
- Modify: `gradle/libs.versions.toml:81-86`

- [ ] **Step 1: Add the plugin to [plugins] section**

After line 85, add:
```toml
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
```

Final [plugins] section should look like:
```toml
[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
```

- [ ] **Step 2: Verify the file was updated correctly**

Run: `cat gradle/libs.versions.toml | grep -A5 "kotlin-serialization"`
Expected: `kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }`

---

## Chunk 2: Update app/build.gradle.kts

### Task 7: Fix kotlin-serialization plugin usage

**Files:**
- Modify: `app/build.gradle.kts:6`

- [ ] **Step 1: Replace hardcoded plugin with version catalog**

Current line 6:
```kotlin
kotlin("plugin.serialization") version "2.1.10"
```

Change to:
```kotlin
alias(libs.plugins.kotlin.serialization)
```

- [ ] **Step 2: Verify the file was updated correctly**

Run: `cat app/build.gradle.kts | head -10 | grep "serialization"`
Expected: `alias(libs.plugins.kotlin.serialization)`

---

## Chunk 3: Build Verification

### Task 8: Sync Gradle and build project

**Files:**
- No file changes - verification only

- [ ] **Step 1: Clean the build**

Run: `./gradlew clean`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Build debug APK**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Run unit tests**

Run: `./gradlew test`
Expected: BUILD SUCCESSFUL with all tests passing

---

### Task 9: Commit the changes

**Files:**
- Modified: `gradle/libs.versions.toml`
- Modified: `app/build.gradle.kts`

- [ ] **Step 1: Stage the files**

Run: `git add gradle/libs.versions.toml app/build.gradle.kts`

- [ ] **Step 2: Commit the changes**

Run: `git commit -m "build: update dependencies to latest versions

- Update Room from 2.6.1 to 2.7.0
- Update Ktor from 3.0.0 to 3.1.1
- Update Kotlinx Serialization from 1.6.3 to 1.7.3
- Update DataStore from 1.1.1 to 1.2.0
- Update Desugar JDK from 2.1.4 to 2.2.0
- Use version catalog for kotlin-serialization plugin"`

Expected: Commit created successfully
