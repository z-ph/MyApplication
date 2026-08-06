# 项目架构图

本文基于当前仓库实现整理，重点覆盖主链路：  
**React + antd-mobile（H5）→ Capacitor 插件 → Facade → Repository/Agent → Android 能力/网络 → 外部服务**。

> Compose 应用内 UI 已在 Phase 4 拆除；唯一保留的原生 UI 是系统级 `FloatingWindowService`（经典 View）。

## 1. 架构总览

```mermaid
flowchart TB
    User["用户"]

    subgraph Entry["入口与应用生命周期"]
        MainActivity["MainActivity\nBridgeActivity"]
        MyApplication["MyApplication"]
        ServiceLocator["ServiceLocator"]
        H5["assets/public\nReact SPA"]
    end

    subgraph Frontend["frontend/ React + antd-mobile"]
        Pages["Chat / Logs / Profile\nPermission / ApiConfig\nSettings / Debug pages"]
        PluginsTS["plugins/*.ts 类型包装"]
        Stores["Zustand useChatStore"]
    end

    subgraph Bridge["原生桥接"]
        CapPlugins["LingxiChat / Agent / ApiConfig\nPermission / Log / Shell / App"]
        Facades["ChatFacade / AgentFacade\nApiConfigFacade / PermissionFacade\nLogFacade / ShellFacade"]
    end

    subgraph Overlay["原生悬浮窗"]
        FloatingWindow["FloatingWindowService"]
    end

    subgraph Data["数据层"]
        ChatRepo["ChatRepository"]
        ApiConfigRepo["ApiConfigRepository"]
        Room["AppDatabase (Room)\nSession / Message / ApiConfig"]
        Prefs["AppPreferences"]
    end

    subgraph Agent["Agent 核心"]
        AgentEngine["LangChainAgentEngine"]
        Tools["AndroidTools"]
        ModelFactory["ModelFactory"]
    end

    subgraph Device["设备能力"]
        AutoService["AutoService\nAccessibilityService"]
        ScreenCapture["ScreenCapture"]
        ScreenService["ScreenCaptureService"]
        ShellExecutor["ShellExecutor"]
        Shizuku["ShizukuHelper"]
    end

    subgraph Network["网络与模型接入"]
        ModelFetcher["ModelFetcher"]
        LangChainSpi["LangChain4jHttpClientSpi"]
        ClientRegistry["LangChainHttpClientRegistry"]
        KtorTransport["KtorHttpClient"]
        HttpProvider["HttpClientProvider"]
        NetworkMonitor["NetworkMonitor"]
    end

    subgraph External["外部系统"]
        AndroidSystem["Android System UI"]
        OtherApps["已安装应用"]
        Providers["LLM Providers"]
    end

    User --> MainActivity
    MainActivity --> H5
    H5 --> Pages
    Pages --> PluginsTS
    PluginsTS --> CapPlugins
    CapPlugins --> Facades

    MyApplication --> ServiceLocator
    MyApplication --> AgentEngine
    ServiceLocator --> Room
    ServiceLocator --> ChatRepo
    ServiceLocator --> ApiConfigRepo
    ServiceLocator --> Prefs
    ServiceLocator --> ModelFetcher
    ServiceLocator --> Facades

    Facades --> ChatRepo
    Facades --> ApiConfigRepo
    Facades --> AgentEngine
    Facades --> FloatingWindow
    Facades --> ShellExecutor
    Facades --> AutoService

    ChatRepo --> Room
    ApiConfigRepo --> Room

    AgentEngine --> Room
    AgentEngine --> ModelFactory
    AgentEngine --> Tools

    Tools --> AutoService
    Tools --> ScreenCapture
    ScreenCapture --> ScreenService
    AutoService --> AndroidSystem
    ScreenService --> AndroidSystem
    Tools --> OtherApps

    ShellExecutor --> Shizuku
    Shizuku --> OtherApps

    ModelFetcher --> HttpProvider
    AgentEngine --> LangChainSpi
    LangChainSpi --> ClientRegistry
    ClientRegistry --> KtorTransport
    KtorTransport --> HttpProvider
    KtorTransport --> NetworkMonitor
    HttpProvider --> Providers

    Stores --> PluginsTS
```

## 2. 核心执行链路

用户在 H5 聊天页发任务 → 插件 → ChatFacade → Agent → 设备自动化。

```mermaid
sequenceDiagram
    participant U as 用户
    participant H5 as ChatPage H5
    participant P as LingxiChatPlugin
    participant CF as ChatFacade
    participant CR as ChatRepository
    participant DB as Room
    participant FW as FloatingWindowService
    participant AE as LangChainAgentEngine
    participant MF as ModelFactory
    participant LLM as LLM Provider
    participant T as AndroidTools
    participant AS as AutoService

    U->>H5: 输入任务并发送
    H5->>P: sendMessage({ content })
    P->>CF: sendMessage(content)
    CF->>CR: 保存用户消息
    CR->>DB: insert(Message/Session)
    CF->>FW: start + onStopButtonClick
    CF->>AE: execute(instruction)
    AE->>DB: 读取 active ApiConfig
    AE->>MF: 创建 ChatModel
    AE->>LLM: 推理 / 工具调用
    AE->>T: AndroidTools
    T->>AS: click/swipe/type/...
    AE-->>CF: callback
    CF->>CR: 保存 AI 消息
    CF-->>P: StateFlow / events
    P-->>H5: messagesChanged / taskProgress
```

## 3. 模块职责

- `frontend/`：React + antd-mobile SPA；路由、页面、Zustand、插件 TS 包装与 web mock。
- `app/.../plugins/`：Capacitor 自定义插件（JSON 编解码 + 协程调度）。
- `app/.../bridge/`：无 UI 依赖的 Facade（Chat / Agent / ApiConfig / Permission / Log / Shell）。
- `app/.../ui/overlay/`：唯一保留原生 UI — `FloatingWindowService` 任务悬浮窗。
- `agent/`：`LangChainAgentEngine` 模型初始化、工具注册、状态流转。
- `accessibility/`：`AutoService` 点击、滑动、输入、控件树。
- `screen/`：MediaProjection 截屏 + `ScreenCaptureService`。
- `data/`：Room 会话 / 消息 / API 配置。
- `network/`：Ktor + LangChain4j SPI + `NetworkMonitor`。
- `shell/`：Shizuku 应用列表 / 启动 / 调试命令。
- `di/`：轻量 `ServiceLocator`。

## 4. 说明

- 应用内 UI **不再使用 Jetpack Compose**；`applicationId` 仍为 `com.example.myapplication`。
- 密钥只经原生 Room / DataStore；H5 列表仅见脱敏 `apiKeyMasked`。
- 主自动化链路：`AndroidTools + AutoService`；Shell 多为调试辅助。
- 契约真源：`docs/bridge-api.md`。迁移清单：`docs/compose-migration-inventory.md`。
