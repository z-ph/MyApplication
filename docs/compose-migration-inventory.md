# Compose → React + antd-mobile 迁移清单（Phase 0）

> **文档-only。** 冻结删除 / 保留范围，供后续 Phase 实施。  
> 不在本阶段删除源码或改应用行为。  
> 契约见：`docs/bridge-api.md`。  
> 计划：`docs/superpowers/plans/2026-08-06-ionic-h5-migration.md`。

**基线分支：** `feat/react-antd-mobile-ui`  
**盘点日期：** 2026-08-06  
**包名 / applicationId：** `com.example.myapplication`（不改）

---

## 1. 信息架构与用户路径（摘要）

主路径：**权限 → 聊天 → API 配置 → 执行任务**。完整说明与插件对应关系见 `docs/bridge-api.md` §1。

| 步骤 | 当前 Compose | 目标路由 |
|------|--------------|----------|
| 权限门闸 | `PermissionScreen` | `/permission` |
| 聊天 / 发任务 | `ChatScreen` + drawer | `/tabs/chat` |
| API 配置 | `ApiConfigScreen` | `/api-config` |
| 执行中展示 | `FloatingWindowService`（原生） | 保留原生 |
| 日志 | `LogScreen` | `/tabs/logs` |
| 我的 | `ProfileScreen`（`MainActivity` 内） | `/tabs/profile` |
| 设置 | `MainScreen` | `/settings` |

---

## 2. 页面映射（Compose → React + antd-mobile）

| Compose | React 路由 | antd-mobile 建议 | 删除时机 |
|---------|------------|------------------|----------|
| `PermissionScreen` | `/permission` | `List` + `Button` + 状态 `Tag` | Phase 4 |
| `ChatScreen` + drawer | `/tabs/chat` | `NavBar` + `Popup` 会话 + 气泡 + `TextArea` | Phase 4（功能 Phase 2） |
| `LogScreen` | `/tabs/logs` | `List` / 虚拟列表 | Phase 4 |
| `ProfileScreen` | `/tabs/profile` | `List` + `List.Item` 箭头 | Phase 4 |
| `MainScreen` | `/settings` | `Form` + `Switch` | Phase 4 |
| `ApiConfigScreen` | `/api-config` | `List` + `Form` in `Popup` | Phase 4 |
| `ApiTestScreen` | `/api-test` | `Form` + 结果区 | Phase 4 |
| `DebugTestScreen` | `/debug` | `Form` + 等宽输出 | Phase 4 |
| `TypeToolTestScreen` | `/type-tool-test` | `Input` / `TextArea` | Phase 4 |
| `FloatingWindowService` | （无 H5 路由） | — | **永不删（本迁移）** |
| `NetworkScreen` | 未在主导航映射 | 现状未挂底栏；迁移时确认是否废弃或并入调试 | Phase 4 评估 |

底栏：antd-mobile `TabBar` → Chat / Logs / 我的（`TabLayout`）。

---

## 3. 待删除清单（Compose UI — 后续 Phase，非本任务）

> 实际删除在 **Task 5 / Phase 4**（`refactor(ui): remove Compose screens and dependencies`）。  
> 删除前须：对应 H5 页与插件已验收；`FloatingWindowService` 与 ChatFacade 同步路径已接通。

### 3.1 聊天 Compose

| 路径 | 说明 |
|------|------|
| `app/src/main/java/com/example/myapplication/ui/chat/ChatScreen.kt` | 聊天页 UI |
| `app/src/main/java/com/example/myapplication/ui/chat/ChatViewModel.kt` | 逻辑下沉至 `ChatFacade` 后删除 |
| `app/src/main/java/com/example/myapplication/ui/chat/components/ChatInputBar.kt` | 输入栏 |
| `app/src/main/java/com/example/myapplication/ui/chat/components/MessageBubble.kt` | 气泡 |
| `app/src/main/java/com/example/myapplication/ui/chat/components/SessionDrawer.kt` | 会话抽屉 |

### 3.2 屏幕 Compose

| 路径 | 说明 |
|------|------|
| `app/src/main/java/com/example/myapplication/ui/screens/PermissionScreen.kt` | 权限页 |
| `app/src/main/java/com/example/myapplication/ui/screens/LogScreen.kt` | 日志页 |
| `app/src/main/java/com/example/myapplication/ui/screens/MainScreen.kt` | 设置页 |
| `app/src/main/java/com/example/myapplication/ui/screens/ApiConfigScreen.kt` | API 配置 UI |
| `app/src/main/java/com/example/myapplication/ui/screens/ApiConfigViewModel.kt` | 逻辑下沉至 `ApiConfigFacade` 后删除 |
| `app/src/main/java/com/example/myapplication/ui/screens/ApiTestScreen.kt` | API 测试 |
| `app/src/main/java/com/example/myapplication/ui/screens/DebugTestScreen.kt` | 调试 |
| `app/src/main/java/com/example/myapplication/ui/screens/TypeToolTestScreen.kt` | 输入工具测试 |
| `app/src/main/java/com/example/myapplication/ui/screens/NetworkScreen.kt` | 未进主导航；随 Compose 清退评估删除 |

### 3.3 主题 Compose

| 路径 | 说明 |
|------|------|
| `app/src/main/java/com/example/myapplication/ui/theme/Color.kt` | Compose 色板 |
| `app/src/main/java/com/example/myapplication/ui/theme/Theme.kt` | `MyApplicationTheme` |
| `app/src/main/java/com/example/myapplication/ui/theme/Type.kt` | 字体 |

### 3.4 MainActivity 内 Compose 导航

| 路径 / 符号 | 说明 |
|-------------|------|
| `app/src/main/java/com/example/myapplication/MainActivity.kt` | **不删除文件**；Phase 1 起改为 `BridgeActivity` 子类，去掉 `setContent { Compose }` |
| `MainApp` | Compose 根导航 — 删除 |
| `ProfileScreen` | 定义在 `MainActivity.kt` 内 — 删除 |
| `AppDestinations` / `ProfileSubPage` | 底栏与子页枚举 — 删除 |
| 相关 Preview | 删除 |

### 3.5 构建依赖（Phase 4 末）

| 项 | 说明 |
|----|------|
| Compose BOM / material3 / ui 等 | 从 `app/build.gradle.kts` / version catalog 移除（确认无残留引用） |
| `buildFeatures.compose` | 关闭 |
| Compose Compiler 插件 | 移除 |

**注意：** 若 `FloatingWindowService` 或其它模块仍间接依赖 Compose，不得盲目移除；当前悬浮窗为经典 View，预期可卸 Compose。

---

## 4. 保留清单

### 4.1 悬浮窗（必须保留）

| 路径 | 说明 |
|------|------|
| `app/src/main/java/com/example/myapplication/ui/overlay/FloatingWindowService.kt` | 任务执行态悬浮展示与停止；经典 View，**非 Compose** |
| `AndroidManifest.xml` 中 `FloatingWindowService` 注册 | 保留 |

### 4.2 悬浮窗专用 drawable（必须保留）

由 `FloatingWindowService` 引用，删除 Compose 时 **不得** 删除：

| 路径 | 用途 |
|------|------|
| `app/src/main/res/drawable/floating_window_bg.xml` | 展开态背景 |
| `app/src/main/res/drawable/floating_window_minimized_bg.xml` | 最小化背景 |
| `app/src/main/res/drawable/status_dot_running.xml` | 运行中状态点 |
| `app/src/main/res/drawable/status_dot_stopped.xml` | 停止/错误状态点 |
| `app/src/main/res/drawable/status_dot_idle.xml` | 空闲状态点 |
| `app/src/main/res/drawable/btn_circle_material.xml` | 圆形按钮背景 |
| `app/src/main/res/drawable/drag_handle_bg.xml` | 拖动手柄 |
| `app/src/main/res/drawable/log_bg.xml` | 悬浮窗日志区背景 |

### 4.3 其它资源（保留，非悬浮窗专用）

| 路径 | 说明 |
|------|------|
| `app/src/main/res/drawable/ic_launcher_background.xml` | 启动图标 |
| `app/src/main/res/drawable/ic_launcher_foreground.xml` | 启动图标 |
| `app/src/main/res/mipmap-*` | 启动图标 |
| 其它 values / xml 系统资源 | 按引用保留 |

### 4.4 必须留在原生层的模块（不删、不迁 JS）

```
accessibility/   AutoService, NodeParser, ActionExecutor
agent/           LangChainAgentEngine, AndroidTools, ModelFactory, RAG*
screen/          ScreenCapture, ScreenCaptureService
shell/           ShellExecutor, ShizukuHelper
data/            Room, Repository, Preferences, mapper
network/         Ktor, CircuitBreaker 相关
api/             ModelFetcher 等
utils/           Logger, CrashHandler 等
di/              ServiceLocator 等
```

### 4.5 迁移期新增（后续任务，本阶段不创建）

| 区域 | 说明 |
|------|------|
| `.../bridge/` | Facade + DTO（Chat / Agent / ApiConfig / Permission / Log） |
| `.../plugins/` | Capacitor Plugin 实现 |
| `frontend/` | React + antd-mobile + TS 插件包装 |
| `app/src/main/assets/public` | H5 构建产物同步目录 |

---

## 5. ViewModel → Facade 搬迁对照（实现指引）

| 现有 | 目标 Facade | 插件 |
|------|-------------|------|
| `ChatViewModel` | `ChatFacade`（含悬浮窗 start/stop/同步） | `LingxiChat` / `LingxiAgent` |
| `ApiConfigViewModel` | `ApiConfigFacade` | `LingxiApiConfig` |
| `PermissionScreen` + `MainActivity.checkPermissions` | `PermissionFacade` | `LingxiPermission` |
| 日志浏览逻辑 | `LogFacade` | `LingxiLog` |

Facade **无** Compose / ViewModel 生命周期依赖；插件只做 JSON 编解码与线程调度。

---

## 6. Appium（策略指针）

- 迁移后：WebView 上下文 + `data-testid`（`chat-input`、`send-btn` 等）
- 改写时机：Task 6 / Phase 5
- 详情：`docs/bridge-api.md` §4
- 现状测试：`tests/appium/`（本阶段不改）

---

## 7. Phase 0 验收勾选

- [x] **0.1** 导出页面信息架构与用户路径（本文 §1 + `bridge-api.md` §1）
- [x] **0.2** 冻结 DTO 与插件方法表为 `docs/bridge-api.md`
- [x] **0.3** Compose 删除清单与保留清单（本文 §3–§4；含 `FloatingWindowService` + drawable）
- [x] **0.4** Appium 策略确认：WebView + `data-testid`（见 `bridge-api.md` §4，实现延后）
- [x] 原生应用行为无变更（仅文档）

---

## 8. 删除顺序建议（Phase 4）

1. H5 全页 + 插件回归通过  
2. 删除 `ui/chat/*`、`ui/screens/*`、`ui/theme/*`  
3. 精简 `MainActivity` 为 Bridge 壳（已在 Phase 1 切换时可只清残留 Compose 符号）  
4. 移除 Compose Gradle 依赖  
5. `compileDebugKotlin` / `assembleDebug` / 关键单测与 Appium 主路径  
6. 更新 `CLAUDE.md` / `AGENTS.md` / `docs/project-architecture.md`
