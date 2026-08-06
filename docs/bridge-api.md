# 灵犀 Bridge API 契约（冻结）

> **Phase 0 冻结文档。** H5（React + antd-mobile）与 Android 原生经 Capacitor 自定义插件通信的唯一真源。  
> 后续 PR 若改方法名、事件名、DTO 字段或语义，**必须同步更新本文**并在 PR 中显式说明。  
> 源计划：`docs/superpowers/plans/2026-08-06-ionic-h5-migration.md` §3.2 / §3.3。

**约束（全局）：**

- JSON 字段统一 **camelCase**
- 应用展示名：灵犀；代码 / JSON 标识符用英文
- `applicationId` 保持 `com.example.myapplication`
- API Key **不进 H5 明文持久化**；读列表返回脱敏字段
- Agent / 无障碍 / 截屏 / Shell 逻辑留在原生
- 页面 **禁止** 直接 `window.Capacitor` 裸调；须经 `frontend/src/plugins/*.ts` 类型包装

---

## 1. 信息架构与主用户路径

### 1.1 当前 Compose 信息架构（基线）

```
MainActivity
└─ 权限未就绪 → PermissionScreen
└─ 权限就绪 → Scaffold + 底栏
   ├─ Chat          → ChatScreen（会话抽屉 / 消息 / 输入）
   ├─ Logs          → LogScreen
   └─ 我的 (Profile)
      ├─ ProfileScreen（状态 + 菜单）
      ├─ MainScreen（设置 / 开关）
      ├─ ApiConfigScreen
      ├─ ApiTestScreen
      ├─ DebugTestScreen
      └─ TypeToolTestScreen

FloatingWindowService（跨应用悬浮层，不在主 UI 导航内）
```

底栏：`Chat` / `Logs` / `我的`。

### 1.2 目标 React 路由（对照）

| 区域 | React 路由 | 说明 |
|------|------------|------|
| 权限 | `/permission` | 门闸；全部就绪后进入聊天 |
| 聊天 | `/tabs/chat` | 主任务入口 |
| 日志 | `/tabs/logs` | 运行日志 |
| 我的 | `/tabs/profile` | 菜单与状态 |
| 设置 | `/settings` | 原 MainScreen |
| API 配置 | `/api-config` | 多 Provider |
| API 测试 | `/api-test` | 调试 |
| 调试 | `/debug` | 调试 |
| 输入工具测试 | `/type-tool-test` | 调试 |
| 悬浮窗 | — | 原生 `FloatingWindowService` 保留 |

底栏：antd-mobile `TabBar` → Chat / Logs / 我的（包在 `TabLayout` 内）。

### 1.3 主用户路径（验收主链路）

```
1. 冷启动
   └─ LingxiPermission.getStatus / refresh
      └─ 未全部就绪 → /permission
         ├─ openAccessibilitySettings
         ├─ requestOverlay
         ├─ requestScreenCapture
         └─ 用户从系统设置返回 → App.resume → refresh → statusChanged

2. 权限就绪
   └─ navigate(/tabs/chat, replace)

3. API 配置（首次或未配置）
   └─ /api-config
      ├─ LingxiApiConfig.create / update（写时传完整 apiKey）
      ├─ setActive
      ├─ testConnection / fetchModels
      └─ 原生 reconfigure Agent → configsChanged

4. 执行任务
   └─ /tabs/chat
      ├─ LingxiChat.createSession / selectSession（可选）
      ├─ sendMessage({ content })
      ├─ 原生：写 Room → LangChainAgentEngine.execute → 无障碍/截屏
      ├─ 事件：messagesChanged / taskProgress / LingxiAgent.stateChanged
      ├─ 同步 FloatingWindowService 展示
      └─ 可选 cancelTask

5. 辅助
   ├─ /tabs/logs → LingxiLog.list / clear / export
   └─ /tabs/profile → 设置、配置、调试入口
```

**数据真源：** Room（会话 / 消息 / API 配置）。H5 只通过插件读写并订阅事件，不自建密钥存储。

---

## 2. DTO 与 JSON 契约

所有示例字段名即为契约字段名；原生序列化 / TypeScript 类型须一致。

### 2.1 ChatMessageDto

统一扁平消息模型（对应原生 `ChatMessage` 密封类映射）。

```json
{
  "id": "uuid",
  "timestamp": 1710000000000,
  "type": "user | ai | toolCall | screenshot | status",
  "content": "文本或状态",
  "isSuccess": true,
  "errorMessage": null,
  "toolName": null,
  "parameters": null,
  "result": null,
  "imageBase64": null,
  "isRunning": false
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | string | 消息 ID |
| `timestamp` | number (epoch ms) | 时间戳 |
| `type` | string | `user` \| `ai` \| `toolCall` \| `screenshot` \| `status` |
| `content` | string | 文本内容；status 类型为状态文案；screenshot 可为描述 |
| `isSuccess` | boolean \| null | ai / toolCall 是否成功 |
| `errorMessage` | string \| null | 失败信息 |
| `toolName` | string \| null | toolCall 工具名 |
| `parameters` | object \| null | toolCall 参数 |
| `result` | string \| null | toolCall 结果 |
| `imageBase64` | string \| null | screenshot / 用户附图 |
| `isRunning` | boolean \| null | status 是否进行中 |

### 2.2 ChatSessionDto

计划 §3.3 TS 示意与原生 `ChatSession` 对齐（Phase 2 实现时冻结扩展须改本文）。

```json
{
  "id": "uuid",
  "title": "新会话",
  "createdAt": 1710000000000,
  "updatedAt": 1710000000000
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | string | 会话 ID |
| `title` | string | 标题 |
| `createdAt` | number (epoch ms) | 创建时间 |
| `updatedAt` | number (epoch ms) | 更新时间 |

### 2.3 AgentStateDto

```json
{
  "state": "IDLE | READY | RUNNING | COMPLETED | ERROR | CANCELLED",
  "step": "",
  "action": "",
  "thinking": "",
  "result": null,
  "error": null
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `state` | string | `IDLE` \| `READY` \| `RUNNING` \| `COMPLETED` \| `ERROR` \| `CANCELLED` |
| `step` | string | 当前步骤描述（可空串） |
| `action` | string | 当前动作（可空串） |
| `thinking` | string | 思考摘要（可空串） |
| `result` | string \| null | 完成结果 |
| `error` | string \| null | 错误信息 |

**原生映射注记（`LangChainAgentEngine.AgentState` → 桥接 DTO）：**

| 侧 | 字段 |
|----|------|
| 原生 Kotlin 现状 | `state`（`AgentStateType`）、`result`、`error`、`timestamp` |
| 桥接 `AgentStateDto` | 上表字段；**不含** `timestamp`（H5 不依赖；若日后需要再加可选字段并回写本文） |

- 原生 **没有** `step` / `action` / `thinking`：Facade 映射时一律填 **`""`**，直至原生侧提供对应字段后再透传。
- 原生 `AgentStateType` 已含 `CANCELLED`；H5 枚举**必须**包含 `CANCELLED`。当前 `cancel()` 实现将状态置为 `IDLE`（未写 `CANCELLED`）：桥接在取消路径上应向 H5 暴露 `CANCELLED`（或在原生 cancel 改为写入 `CANCELLED` 后原样透传），保证 H5 能区分「空闲」与「用户取消」。
- **`LingxiAgent.getState` 返回裸 `AgentStateDto` 对象**，禁止再包一层 `{ "state": AgentStateDto }`（避免与字段名 `state` 混淆）。`stateChanged` 事件载荷同为裸 `AgentStateDto`。

### 2.4 ApiConfigDto

```json
{
  "id": "uuid",
  "name": "默认",
  "provider": "OPENAI | ZHIPU | ...",
  "apiKeyMasked": "sk-***",
  "baseUrl": "https://...",
  "modelId": "gpt-4o-mini",
  "isActive": true
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | string | 配置 ID |
| `name` | string | 显示名 |
| `provider` | string | 桥接侧 **大写枚举风格** 字符串（如 `OPENAI`、`ZHIPU`）；见下方映射表 |
| `apiKeyMasked` | string | **读列表时**脱敏密钥；**禁止**在 list 响应中返回完整 key |
| `baseUrl` | string | API Base URL |
| `modelId` | string | 模型 ID |
| `isActive` | boolean | 是否当前活跃配置 |

**`provider` 与 Room / 原生映射：**

- Room 实体 `ApiConfigEntity.providerId` 存 **小写 id**（如 `openai`、`zhipu`），与 `ModelProvider.id` 一致。
- 桥接 JSON 字段名固定为 **`provider`**（不是 `providerId`），值为 **大写枚举风格** 字符串（与 `ModelProvider` 枚举名对齐）。
- Facade **双向映射**：读出 `providerId` → `provider`；写入 `provider` → `providerId`。

| 桥接 `provider` | Room / `ModelProvider.id` | `ModelProvider` 枚举 |
|-----------------|---------------------------|----------------------|
| `ZHIPU` | `zhipu` | `ZHIPU` |
| `OPENAI` | `openai` | `OPENAI` |
| `DEEPSEEK` | `deepseek` | `DEEPSEEK` |
| `QWEN` | `qwen` | `QWEN` |
| `ANTHROPIC` | `anthropic` | `ANTHROPIC` |
| `OLLAMA` | `ollama` | `OLLAMA` |
| `AZURE_OPENAI` | `azure-openai` | `AZURE_OPENAI` |
| `GOOGLE_VERTEX` | `google-vertex` | `GOOGLE_VERTEX` |
| `MISTRAL` | `mistral` | `MISTRAL` |
| `HUGGING_FACE` | `huggingface` | `HUGGING_FACE` |
| `CUSTOM` | `custom` | `CUSTOM` |

未知 `providerId`：实现侧应拒绝或回落策略与 `ModelProvider.fromId` 一致，并在插件错误信息中可诊断；**勿**静默写成错误枚举而不回写文档。

**写配置：** `create` / `update` 请求体中 H5 传完整 `apiKey`（及必要字段）；原生写入 Room / 安全存储后，响应与 list 仅返回 `apiKeyMasked`。

写请求补充字段（与 list DTO 区分，实现时以插件方法参数为准）：

```json
{
  "id": "uuid",
  "name": "默认",
  "provider": "OPENAI",
  "apiKey": "sk-完整密钥仅写路径",
  "baseUrl": "https://...",
  "modelId": "gpt-4o-mini"
}
```

### 2.5 响应信封（冻结）

**规则（已钉死，实现不得二选一）：**

1. **集合列表方法**一律返回 **对象包装**，禁止裸数组。
2. **`LingxiAgent.getState`** 返回 **裸 `AgentStateDto`**（单对象，无外层 key）。
3. **事件载荷**在适用处与对应 list 方法形状一致（见各插件事件表）。

| 场景 | 冻结形状 | 禁止 |
|------|----------|------|
| 会话列表 | `{ "sessions": ChatSessionDto[] }` | 裸 `ChatSessionDto[]` |
| 单会话 | `{ "session": ChatSessionDto }` | — |
| 消息列表 | `{ "messages": ChatMessageDto[] }` | 裸数组 |
| 配置列表 | `{ "configs": ApiConfigDto[] }` | 裸 `ApiConfigDto[]` |
| 日志列表 | `{ "logs": LogEntryDto[] }` | 裸数组（`LogEntryDto` 字段实现时回写 §2） |
| Agent 状态（`getState` / `stateChanged`） | **裸** `AgentStateDto` | `{ "state": AgentStateDto }` 双层包装 |
| 权限状态 | 见 `LingxiPermission.getStatus`（字段在实现插件时补充并回写本文） | — |
| 版本 | `{ "version": string, ... }` | — |

**事件与 list 对齐（冻结）：**

| 事件 | 载荷形状 |
|------|----------|
| `LingxiChat.sessionsChanged` | `{ "sessions": ChatSessionDto[] }`（与 `listSessions` 一致；禁止仅空通知而无约定） |
| `LingxiChat.messagesChanged` | `{ "sessionId": string, "messages": ChatMessageDto[] }`（`messages` 必填，与 list 同形） |
| `LingxiAgent.stateChanged` | 裸 `AgentStateDto`（与 `getState` 一致） |
| `LingxiApiConfig.configsChanged` | `{ "configs": ApiConfigDto[] }`（与 `list` 一致） |
| `LingxiLog.logAppended` | `{ "logs": LogEntryDto[] }` 或单条时仍可用 `{ "logs": [one] }`（与 `list` 同键名） |

**变更规则：** 新增可选字段可向后兼容；重命名 / 删除 / 改语义 / 改信封必须升文档版本说明并同步 TS 与 Facade。

---

## 3. Capacitor 自定义插件清单

TypeScript 包装路径约定：`frontend/src/plugins/*.ts`。  
页面只 import 包装模块，禁止直接访问 `window.Capacitor.Plugins`。

### 3.1 `LingxiChat`

| 方法 | 示意参数 | 示意返回（冻结） | 说明 |
|------|----------|------------------|------|
| `listSessions` | — | `{ sessions: ChatSessionDto[] }` | 会话列表；**禁止裸数组** |
| `createSession` | `{ title?: string }` | `{ session: ChatSessionDto }` | 创建会话 |
| `selectSession` | `{ sessionId: string }` | `void` | 切换当前会话 |
| `deleteSession` | `{ sessionId: string }` | `void` | 删除会话 |
| `listMessages` | `{ sessionId: string }` | `{ messages: ChatMessageDto[] }` | 消息列表；**禁止裸数组** |
| `sendMessage` | `{ content: string }` | `void` | 发送用户指令并启动任务 |
| `cancelTask` | — | `void` | 取消进行中任务（状态见 §2.3 `CANCELLED`） |
| `clearMessages` | — | `void` | 清空当前会话消息（语义以 Facade 为准） |

| 事件 | 载荷（冻结） | 说明 |
|------|--------------|------|
| `sessionsChanged` | `{ sessions: ChatSessionDto[] }` | 与 `listSessions` 同形 |
| `messagesChanged` | `{ sessionId: string, messages: ChatMessageDto[] }` | `messages` 必填，与 `listMessages` 同形 |
| `taskProgress` | 进度摘要 / 与 Agent 态相关 payload | 任务进度推送（非 list 信封；实现时回写字段） |

### 3.2 `LingxiAgent`

| 方法 | 示意返回（冻结） | 说明 |
|------|------------------|------|
| `getState` | **裸** `AgentStateDto` | **禁止** `{ state: AgentStateDto }` 双层包装 |
| `reconfigure` | 实现选定（建议 `void` 或 `{ ok: boolean }`） | 按活跃 API 配置重建 / 重载 Agent |
| `isConfigured` | `{ configured: boolean }` 或实现回写 | 是否已具备可运行配置 |

| 事件 | 载荷（冻结） | 说明 |
|------|--------------|------|
| `stateChanged` | **裸** `AgentStateDto` | 与 `getState` 同形；建议原生 throttle 100–200ms |

### 3.3 `LingxiApiConfig`

| 方法 | 示意返回（冻结） | 说明 |
|------|------------------|------|
| `list` | `{ configs: ApiConfigDto[] }` | **禁止裸数组**；含 `apiKeyMasked`，无明文 key |
| `create` | `{ config: ApiConfigDto }`（建议） | 新建（请求含完整 `apiKey`） |
| `update` | `{ config: ApiConfigDto }`（建议） | 更新（可含完整 `apiKey`） |
| `delete` | `void` | 删除 |
| `setActive` | `void` | 设为活跃配置并触发 reconfigure |
| `fetchModels` | 实现时回写（建议 `{ models: string[] }`） | 拉取可用模型列表 |
| `testConnection` | 实现时回写 | 测连 |

| 事件 | 载荷（冻结） | 说明 |
|------|--------------|------|
| `configsChanged` | `{ configs: ApiConfigDto[] }` | 与 `list` 同形 |

### 3.4 `LingxiPermission`

| 方法 | 说明 |
|------|------|
| `getStatus` | 查询无障碍 / 悬浮窗 / 截屏 / 应用列表等状态 |
| `openAccessibilitySettings` | 跳转系统无障碍设置 |
| `requestOverlay` | 请求悬浮窗权限（跳转设置） |
| `requestScreenCapture` | 请求 MediaProjection（经 Activity Result） |
| `refresh` | 重新检测并推送 `statusChanged` |

| 事件 | 说明 |
|------|------|
| `statusChanged` | 权限状态更新 |

**实现注记（计划 §3.5）：** `requestScreenCapture` 由 `MainActivity`（未来 `BridgeActivity`）持有 launcher；无障碍与悬浮窗仅能打开系统设置，用户返回后 H5 在路由进入 / `App.resume` 时调用 `refresh`。

### 3.5 `LingxiLog`

| 方法 | 示意返回（冻结） | 说明 |
|------|------------------|------|
| `list` | `{ logs: LogEntryDto[] }` | **禁止裸数组**；`LogEntryDto` 字段实现时回写 §2 |
| `clear` | `void` | 清空 |
| `export` | 实现时回写（路径 / 文本） | 导出 |

| 事件 | 载荷（冻结） | 说明 |
|------|--------------|------|
| `logAppended` | `{ logs: LogEntryDto[] }` | 与 list 同键名；单条用单元素数组 |

### 3.6 `LingxiShell`（调试）

| 方法 | 说明 |
|------|------|
| `runCommand` | 执行 shell 命令 |
| `listPackages` | 包列表 |

无事件。

### 3.7 `LingxiApp`

| 方法 | 说明 |
|------|------|
| `echo` | 探活：入参 `{ value: string }`，回 `{ value: string }`（原样返回） |
| `getVersion` | 应用版本等信息 → `{ appName, packageName, versionName, versionCode, mock? }` |
| `openUrl` | 打开外部 URL → 入参 `{ url: string }` |

无事件。

```typescript
// frontend/src/plugins/lingxi-app.ts
export interface LingxiAppPlugin {
  echo(options: { value: string }): Promise<{ value: string }>;
  getVersion(): Promise<{
    appName: string;
    packageName: string;
    versionName: string;
    versionCode: number;
    mock?: boolean;
  }>;
  openUrl(options: { url: string }): Promise<void>;
}
```

### 3.8 TypeScript 包装示意（Chat）

```typescript
// frontend/src/plugins/lingxi-chat.ts（示意，实现见后续 Phase）
export interface LingxiChatPlugin {
  listSessions(): Promise<{ sessions: ChatSessionDto[] }>;
  createSession(options: { title?: string }): Promise<{ session: ChatSessionDto }>;
  selectSession(options: { sessionId: string }): Promise<void>;
  deleteSession(options: { sessionId: string }): Promise<void>;
  listMessages(options: { sessionId: string }): Promise<{ messages: ChatMessageDto[] }>;
  sendMessage(options: { content: string }): Promise<void>;
  cancelTask(): Promise<void>;
  clearMessages(): Promise<void>;
  addListener(
    eventName: 'sessionsChanged' | 'messagesChanged' | 'taskProgress',
    listener: (data: unknown) => void
  ): Promise<PluginListenerHandle>;
}
```

---

## 4. Appium 选择器策略（Phase 5 / Task 6）

**现状：** `tests/appium/` 基于原生 Compose/Android UIAutomator 选择器（如 `ChatScreenPage`）。

**迁移后策略（冻结意图，实现推迟到 Task 6 / Phase 5）：**

1. **WebView 上下文：** 切换到 Capacitor WebView context 后再查找 DOM。
2. **稳定选择器：** 关键控件使用 `data-testid`，至少包括：
   - `chat-input` — 聊天输入框
   - `send-btn` — 发送按钮
3. 页面对象改写为 WebView + CSS/`data-testid` 定位；包名 `com.example.myapplication` 不变。
4. 在 H5 落地聊天页时即预埋 `data-testid`，避免 Phase 5 大改 DOM。

**本 Phase：** 仅文档确认策略；不改测试代码、不改应用行为。

---

## 5. 安全与非功能约定

| 项 | 约定 |
|----|------|
| 密钥 | list / 事件载荷只用 `apiKeyMasked`；完整 `apiKey` 仅 create/update 请求体 |
| H5 存储 | 不把 API Key 写入 `localStorage` / 明文 DataStore 经 JS |
| 事件风暴 | Agent 高频状态原生侧 throttle（建议 100–200ms） |
| 真源 | Room 为会话/消息/配置真源；H5 订阅 + 必要时 re-fetch |
| 悬浮窗 | 不经 H5 路由；由原生 Service 与 Chat/Agent Facade 同步 |

---

## 6. 文档维护

| 变更类型 | 动作 |
|----------|------|
| 新增插件方法 / 事件 | 更新 §3 对应表 + TS 包装 |
| DTO 字段增减 | 更新 §2 示例与字段表 |
| 路由 / 主路径变化 | 更新 §1 |
| Appium testid 增减 | 更新 §4 |

**相关文档：**

- 迁移文件清单：`docs/compose-migration-inventory.md`
- 实施计划：`docs/superpowers/plans/2026-08-06-ionic-h5-migration.md`
