# 项目架构图

本文基于当前仓库实现整理，重点覆盖实际接入的主链路：`Compose UI -> ViewModel -> Repository/Agent -> Android 能力/网络 -> 外部服务`。

## 1. 架构总览

```mermaid
flowchart TB
    User["用户"]

    subgraph Entry["入口与应用生命周期"]
        MainActivity["MainActivity"]
        MyApplication["MyApplication"]
        ServiceLocator["ServiceLocator"]
    end

    subgraph UI["UI / Compose"]
        PermissionScreen["PermissionScreen"]
        ChatScreen["ChatScreen"]
        SettingsScreens["MainScreen / ApiConfigScreen / ApiTestScreen / DebugTestScreen"]
        FloatingWindow["FloatingWindowService"]
    end

    subgraph VM["ViewModel"]
        ChatVM["ChatViewModel"]
        ApiConfigVM["ApiConfigViewModel"]
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
        Providers["LLM Providers\nOpenAI / Zhipu / Qwen / DeepSeek / Ollama / ..."]
    end

    User --> MainActivity
    MyApplication --> ServiceLocator
    MyApplication --> AgentEngine
    ServiceLocator --> Room
    ServiceLocator --> ChatRepo
    ServiceLocator --> ApiConfigRepo
    ServiceLocator --> Prefs
    ServiceLocator --> ModelFetcher

    MainActivity --> PermissionScreen
    MainActivity --> ChatScreen
    MainActivity --> SettingsScreens

    ChatScreen --> ChatVM
    SettingsScreens --> ApiConfigVM
    ChatVM --> FloatingWindow

    ChatVM --> ChatRepo
    ChatRepo --> Room

    ApiConfigVM --> ApiConfigRepo
    ApiConfigRepo --> Room
    ApiConfigVM --> ModelFetcher

    ChatVM --> AgentEngine
    ApiConfigVM --> AgentEngine
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
    SettingsScreens -. 调试/辅助能力 .-> ShellExecutor

    ModelFetcher --> HttpProvider
    AgentEngine --> LangChainSpi
    LangChainSpi --> ClientRegistry
    ClientRegistry --> KtorTransport
    KtorTransport --> HttpProvider
    KtorTransport --> NetworkMonitor
    HttpProvider --> Providers
```

## 2. 核心执行链路

下面这条链路对应“用户在聊天页发送一句话，Agent 判断并执行手机自动化”。

```mermaid
sequenceDiagram
    participant U as 用户
    participant CS as ChatScreen
    participant VM as ChatViewModel
    participant CR as ChatRepository
    participant DB as Room
    participant FW as FloatingWindowService
    participant AE as LangChainAgentEngine
    participant MF as ModelFactory
    participant LLM as LLM Provider
    participant T as AndroidTools
    participant AS as AutoService
    participant SM as ScreenCapture
    participant SC as ScreenCaptureService
    participant OS as Android System
    participant APP as Other Apps

    U->>CS: 输入任务并发送
    CS->>VM: sendMessage(content)
    VM->>CR: 保存用户消息
    CR->>DB: insert(Message/Session)

    VM->>FW: start/show
    VM->>AE: execute(instruction)
    AE->>DB: 读取 active ApiConfig
    AE->>MF: 创建 ChatModel
    AE->>LLM: 发起推理请求
    LLM-->>AE: 返回工具调用/结果

    AE->>T: 调用 AndroidTools
    alt 无障碍操作
        T->>AS: click/swipe/type/findNode
        AS->>AS: 读取当前控件树并执行手势
    else 屏幕观察
        T->>SM: capture()
        SM->>SC: capture()
        SC-->>SM: bitmap/frame
        SM-->>T: bitmap/frame
    else 应用启动
        T->>OS: startActivity / PackageManager
        OS->>APP: 打开目标应用
    end

    AE-->>VM: callback(result)
    VM->>CR: 保存 AI 消息/完成状态
    CR->>DB: insert(Message)
    VM->>FW: 更新状态并隐藏
    VM-->>CS: StateFlow 刷新 UI
```

## 3. 模块职责

- `ui/`：Compose 页面与组件，负责权限引导、聊天、配置管理、调试页。
- `ui/chat/`：聊天主入口，`ChatViewModel` 负责会话、消息、Agent 执行和悬浮窗联动。
- `agent/`：Agent 运行时；`LangChainAgentEngine` 负责模型初始化、工具注册、状态流转。
- `agent/langchain/`：模型工厂与 LangChain 相关适配；`RAGManager` 当前存在但处于禁用状态。
- `accessibility/`：`AutoService` 提供点击、滑动、输入、控件树检索等自动化能力。
- `screen/`：基于 `MediaProjection` 的屏幕捕获，使用前台服务 `ScreenCaptureService` 承载。
- `data/`：Room 持久化层，存储聊天会话、消息、API 配置。
- `network/`：统一 HTTP 基础设施；一条给 `ModelFetcher` 用，另一条通过 LangChain4j SPI 接给 Agent。
- `shell/`：Shizuku 辅助能力，主要用于更可靠的应用列表、应用启动和调试。
- `di/`：当前使用轻量级 `ServiceLocator`，尚未引入 Hilt/Koin。

## 4. 说明

- 当前主业务链路是“聊天驱动的手机自动化”，不是传统的 MVC 页面应用。
- `ShellExecutor` 目前更多出现在调试/辅助场景，主自动化链路仍以 `AndroidTools + AutoService` 为核心。
- `RAGManager` 虽保留在代码中，但当前版本没有真正接入运行链路。
