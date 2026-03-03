# LLM API 端点格式对比分析

**版本**: v1.0  
**更新日期**: 2026-03-03

---

## 一、主流 API 端点格式总览

| 提供商 | 端点路径 | 请求格式 | 响应格式 | OpenAI 兼容 |
|--------|----------|----------|----------|-------------|
| **OpenAI** | `/v1/chat/completions` | OpenAI | OpenAI | ✅ 标准 |
| **智谱 AI** | `/api/paas/v4/chat/completions` | OpenAI | OpenAI | ✅ 兼容 |
| **Azure OpenAI** | `/openai/deployments/{id}/chat/completions` | OpenAI | OpenAI | ✅ 兼容 |
| **Mistral AI** | `/v1/chat/completions` | OpenAI | OpenAI | ✅ 兼容 |
| **Groq** | `/v1/chat/completions` | OpenAI | OpenAI | ✅ 兼容 |
| **Anthropic** | `/v1/messages` | Anthropic | Anthropic | ❌ 独立 |
| **Google Vertex** | `/v1/projects/{p}/locations/{l}/publishers/google/models/{m}:predict` | Google | Google | ❌ 独立 |
| **Ollama** | `/api/chat` | Ollama | Ollama | ❌ 独立 |
| **HuggingFace** | `/models/{model_id}` | HF | HF | ❌ 独立 |

---

## 二、端点格式分类

### 📌 类型 1: OpenAI 兼容格式（最主流）

**采用厂商**: OpenAI, 智谱 AI, Azure OpenAI, Mistral, Groq, Moonshot, MiniMax, 零一万物等

#### 端点结构
```
POST {baseUrl}/chat/completions
或
POST {baseUrl}/v1/chat/completions
```

#### 请求格式
```json
{
  "model": "gpt-4o",
  "messages": [
    {
      "role": "system",
      "content": "你是一个助手"
    },
    {
      "role": "user",
      "content": "你好"
    }
  ],
  "temperature": 0.7,
  "max_tokens": 2048,
  "stream": false,
  "tools": [
    {
      "type": "function",
      "function": {
        "name": "click",
        "description": "点击屏幕",
        "parameters": {
          "type": "object",
          "properties": {
            "x": {"type": "number"},
            "y": {"type": "number"}
          },
          "required": ["x", "y"]
        }
      }
    }
  ],
  "tool_choice": "auto"
}
```

#### 响应格式
```json
{
  "id": "chatcmpl-xxx",
  "object": "chat.completion",
  "created": 1234567890,
  "model": "gpt-4o",
  "choices": [
    {
      "index": 0,
      "message": {
        "role": "assistant",
        "content": "你好！有什么可以帮助你的？",
        "tool_calls": [
          {
            "id": "call_xxx",
            "type": "function",
            "function": {
              "name": "click",
              "arguments": "{\"x\": 100, \"y\": 200}"
            }
          }
        ]
      },
      "finish_reason": "tool_calls"
    }
  ],
  "usage": {
    "prompt_tokens": 50,
    "completion_tokens": 30,
    "total_tokens": 80
  }
}
```

#### 认证头
```http
Authorization: Bearer {api_key}
Content-Type: application/json
```

---

### 📌 类型 2: Anthropic 格式

**采用厂商**: Anthropic (Claude)

#### 端点结构
```
POST {baseUrl}/v1/messages
```

#### 请求格式
```json
{
  "model": "claude-3-5-sonnet-20241022",
  "max_tokens": 2048,
  "messages": [
    {
      "role": "user",
      "content": [
        {
          "type": "text",
          "text": "你好"
        },
        {
          "type": "image",
          "source": {
            "type": "base64",
            "media_type": "image/jpeg",
            "data": "base64_encoded_image"
          }
        }
      ]
    }
  ],
  "tools": [
    {
      "name": "click",
      "description": "点击屏幕",
      "input_schema": {
        "type": "object",
        "properties": {
          "x": {"type": "number"},
          "y": {"type": "number"}
        },
        "required": ["x", "y"]
      }
    }
  ],
  "tool_choice": {"type": "auto"}
}
```

**关键差异**:
- 没有 `system` 角色，system prompt 放在请求顶层
- `messages` 内容是数组，支持多模态
- 工具定义用 `input_schema` 而非 `parameters`
- 响应中 `tool_calls` 叫 `tool_use`

#### 响应格式
```json
{
  "id": "msg_xxx",
  "type": "message",
  "role": "assistant",
  "content": [
    {
      "type": "text",
      "text": "我来帮你点击"
    },
    {
      "type": "tool_use",
      "id": "tool_xxx",
      "name": "click",
      "input": {
        "x": 100,
        "y": 200
      }
    }
  ],
  "stop_reason": "tool_use",
  "usage": {
    "input_tokens": 50,
    "output_tokens": 30
  }
}
```

#### 认证头
```http
x-api-key: {api_key}
anthropic-version: 2023-06-01
Content-Type: application/json
```

---

### 📌 类型 3: Ollama 格式（自部署）

**采用厂商**: Ollama, LocalAI

#### 端点结构
```
POST {baseUrl}/api/chat
或
POST {baseUrl}/v1/chat/completions  (兼容模式)
```

#### 请求格式
```json
{
  "model": "llama3.2-vision",
  "messages": [
    {
      "role": "user",
      "content": "你好",
      "images": ["base64_image_1", "base64_image_2"]
    }
  ],
  "stream": false,
  "options": {
    "temperature": 0.7,
    "num_predict": 2048
  }
}
```

#### 响应格式
```json
{
  "model": "llama3.2-vision",
  "created_at": "2024-01-01T00:00:00Z",
  "message": {
    "role": "assistant",
    "content": "你好！有什么可以帮助你的？",
    "tool_calls": [
      {
        "function": {
          "name": "click",
          "arguments": {
            "x": 100,
            "y": 200
          }
        }
      }
    ]
  },
  "done": true,
  "total_duration": 1234567890,
  "eval_count": 50,
  "eval_duration": 987654321
}
```

#### 认证头
```http
Content-Type: application/json
# 默认无需认证，可自定义
```

---

### 📌 类型 4: Google Vertex AI 格式

**采用厂商**: Google Cloud Vertex AI, Google AI Studio

#### 端点结构
```
POST https://{location}-aiplatform.googleapis.com/v1/projects/{project}/locations/{location}/publishers/google/models/{model}:predict
或简化版:
POST https://generativelanguage.googleapis.com/v1/models/{model}:generateContent
```

#### 请求格式 (Gemini API)
```json
{
  "contents": [
    {
      "role": "user",
      "parts": [
        {
          "text": "你好"
        },
        {
          "inline_data": {
            "mime_type": "image/jpeg",
            "data": "base64_image"
          }
        }
      ]
    }
  ],
  "generationConfig": {
    "temperature": 0.7,
    "maxOutputTokens": 2048
  },
  "tools": [
    {
      "functionDeclarations": [
        {
          "name": "click",
          "description": "点击屏幕",
          "parameters": {
            "type": "object",
            "properties": {
              "x": {"type": "number"},
              "y": {"type": "number"}
            }
          }
        }
      ]
    }
  ]
}
```

#### 响应格式
```json
{
  "candidates": [
    {
      "content": {
        "role": "model",
        "parts": [
          {
            "text": "我来帮你"
          },
          {
            "functionCall": {
              "name": "click",
              "args": {
                "x": 100,
                "y": 200
              }
            }
          }
        ]
      },
      "finishReason": "STOP"
    }
  ],
  "usageMetadata": {
    "promptTokenCount": 50,
    "candidatesTokenCount": 30,
    "totalTokenCount": 80
  }
}
```

#### 认证头
```http
Authorization: Bearer {oauth2_access_token}
Content-Type: application/json
# 或使用 API Key
?alt=sse&key={api_key}
```

---

### 📌 类型 5: Hugging Face Inference 格式

**采用厂商**: Hugging Face

#### 端点结构
```
POST https://api-inference.huggingface.co/models/{model_id}
或
POST https://api-inference.huggingface.co/models/{model_id}/v1/chat/completions (兼容模式)
```

#### 请求格式 (原生)
```json
{
  "inputs": "你好，请帮我",
  "parameters": {
    "max_new_tokens": 2048,
    "temperature": 0.7,
    "return_full_text": false
  },
  "options": {
    "wait_for_model": true
  }
}
```

#### 响应格式
```json
[
  {
    "generated_text": "你好！有什么可以帮助你的？"
  }
]
```

---

## 三、端点格式差异对比表

### 3.1 URL 路径差异

| 格式类型 | 路径模式 | 示例 |
|----------|----------|------|
| OpenAI 兼容 | `/v1/chat/completions` | `https://api.openai.com/v1/chat/completions` |
| OpenAI 兼容 (智谱) | `/api/paas/v4/chat/completions` | `https://open.bigmodel.cn/api/paas/v4/chat/completions` |
| OpenAI 兼容 (Azure) | `/openai/deployments/{id}/chat/completions` | `https://xxx.openai.azure.com/openai/deployments/gpt-4/chat/completions` |
| Anthropic | `/v1/messages` | `https://api.anthropic.com/v1/messages` |
| Ollama | `/api/chat` | `http://localhost:11434/api/chat` |
| Google Gemini | `/v1/models/{model}:generateContent` | `https://generativelanguage.googleapis.com/v1/models/gemini-pro:generateContent` |
| Google Vertex | `/v1/projects/{p}/locations/{l}/publishers/google/models/{m}:predict` | 复杂路径 |
| Hugging Face | `/models/{model_id}` | `https://api-inference.huggingface.co/models/meta-llama/Llama-2-7b` |

### 3.2 请求体差异

| 字段 | OpenAI | Anthropic | Ollama | Google |
|------|--------|-----------|--------|--------|
| 模型名 | `model` | `model` | `model` | 在 URL 中 |
| 消息 | `messages[]` | `messages[]` | `messages[]` | `contents[]` |
| 系统提示 | `messages[{role:system}]` | `system` (顶层) | `messages[{role:system}]` | `system_instruction` |
| 温度 | `temperature` | `temperature` | `options.temperature` | `generationConfig.temperature` |
| 最大 token | `max_tokens` | `max_tokens` | `options.num_predict` | `generationConfig.maxOutputTokens` |
| 工具定义 | `tools[].function` | `tools[]` | `tools[]` | `tools[].functionDeclarations` |
| 工具参数 | `parameters` | `input_schema` | `parameters` | `parameters` |
| 图片 | `messages[].content[]` | `messages[].content[]` | `messages[].images[]` | `contents[].parts[]` |

### 3.3 响应体差异

| 字段 | OpenAI | Anthropic | Ollama | Google |
|------|--------|-----------|--------|--------|
| 响应 ID | `id` | `id` | 无 | 无 |
| 模型 | `model` | `model` | `model` | 在请求中 |
| 消息内容 | `choices[].message` | `content[]` | `message` | `candidates[].content` |
| 工具调用 | `tool_calls[]` | `tool_use[]` | `tool_calls[]` | `functionCall` |
| 完成原因 | `finish_reason` | `stop_reason` | `done` | `finishReason` |
| Token 统计 | `usage` | `usage` | `eval_count` | `usageMetadata` |

### 3.4 认证头差异

| 提供商 | 认证头格式 | 额外头 |
|--------|------------|--------|
| OpenAI | `Authorization: Bearer {key}` | - |
| 智谱 AI | `Authorization: Bearer {key}` | - |
| Azure OpenAI | `api-key: {key}` | - |
| Anthropic | `x-api-key: {key}` | `anthropic-version: 2023-06-01` |
| Ollama | 无 (或自定义) | - |
| Google | `Authorization: Bearer {oauth_token}` | - |
| Hugging Face | `Authorization: Bearer {key}` | - |

---

## 四、共同点总结

### 4.1 所有 API 的共同特征

1. **HTTP 方法**: 全部使用 `POST`
2. **内容类型**: 全部使用 `application/json`
3. **基础结构**: 请求→响应的同步模式（可选流式）
4. **认证方式**: 大多使用 Bearer Token 或 API Key
5. **错误处理**: 返回 HTTP 状态码 + JSON 错误体

### 4.2 消息结构的共同点

```
所有 API 都支持某种形式的对话历史:

OpenAI:     messages: [{role, content}, ...]
Anthropic:  messages: [{role, content}, ...]
Ollama:     messages: [{role, content}, ...]
Google:     contents: [{role, parts}, ...]

角色类型大多支持:
- user (用户消息)
- assistant/模型 (AI 响应)
- system (系统提示，位置可能不同)
```

### 4.3 工具/函数调用的共同点

```
所有支持工具调用的 API 都包含:

1. 工具定义:
   - 名称 (name)
   - 描述 (description)
   - 参数 schema (parameters/input_schema)

2. 工具调用响应:
   - 工具 ID (id/call_id)
   - 工具名称 (name)
   - 参数 (arguments/input/args)
```

### 4.4 多模态支持的共同点

```
所有支持图像的 API 都提供:

1. Base64 编码图片数据
2. MIME 类型声明 (image/jpeg, image/png)
3. 图片与文本的混合输入
```

---

## 五、统一适配层设计

### 5.1 统一请求接口

```kotlin
interface UnifiedChatApi {
    suspend fun chat(request: UnifiedChatRequest): UnifiedChatResponse
}

data class UnifiedChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Float = 0.7f,
    val maxTokens: Int = 2048,
    val tools: List<ToolDefinition>? = null,
    val systemPrompt: String? = null
)

data class ChatMessage(
    val role: Role,  // USER, ASSISTANT, SYSTEM
    val content: String,
    val images: List<String>? = null  // base64
)

data class ToolDefinition(
    val name: String,
    val description: String,
    val parameters: JsonSchema
)
```

### 5.2 提供商适配器

```kotlin
interface ProviderAdapter {
    fun buildUrl(config: ProviderConfig): String
    fun buildHeaders(config: ProviderConfig): Map<String, String>
    fun transformRequest(unified: UnifiedChatRequest): Any
    fun transformResponse(raw: Any): UnifiedChatResponse
}

class OpenAiAdapter : ProviderAdapter { /* ... */ }
class AnthropicAdapter : ProviderAdapter { /* ... */ }
class OllamaAdapter : ProviderAdapter { /* ... */ }
class GoogleAdapter : ProviderAdapter { /* ... */ }
```

### 5.3 端点构建器

```kotlin
object EndpointBuilder {
    
    /**
     * 根据提供商配置构建完整 URL
     */
    fun buildUrl(
        provider: ModelProvider,
        baseUrl: String,
        customEndpoint: String? = null
    ): String {
        return when (provider) {
            ModelProvider.OPENAI,
            ModelProvider.MISTRAL,
            ModelProvider.GROQ,
            ModelProvider.ZHIPU -> {
                // OpenAI 兼容：{baseUrl}/chat/completions
                val endpoint = customEndpoint ?: "/chat/completions"
                normalizeUrl(baseUrl, endpoint)
            }
            ModelProvider.ANTHROPIC -> {
                // Anthropic: {baseUrl}/v1/messages
                val endpoint = customEndpoint ?: "/v1/messages"
                normalizeUrl(baseUrl, endpoint)
            }
            ModelProvider.OLLAMA -> {
                // Ollama: {baseUrl}/api/chat
                val endpoint = customEndpoint ?: "/api/chat"
                normalizeUrl(baseUrl, endpoint)
            }
            ModelProvider.GOOGLE_VERTEX -> {
                // Google: {baseUrl}/v1/models/{model}:generateContent
                // 需要 model 参数
                baseUrl  // Google 的 baseUrl 已包含完整路径
            }
            ModelProvider.AZURE_OPENAI -> {
                // Azure: {baseUrl}/openai/deployments/{deployment}/chat/completions?api-version=xxx
                // baseUrl 应该包含 deployment 信息
                val version = customEndpoint ?: "2024-02-15-preview"
                "$baseUrl?api-version=$version"
            }
            ModelProvider.CUSTOM -> {
                // 自定义：直接使用 baseUrl + endpoint
                val endpoint = customEndpoint ?: "/chat/completions"
                normalizeUrl(baseUrl, endpoint)
            }
        }
    }
    
    private fun normalizeUrl(base: String, endpoint: String): String {
        val baseNormalized = base.trimEnd('/')
        val endpointNormalized = endpoint.trimStart('/')
        return "$baseNormalized/$endpointNormalized"
    }
}
```

---

## 六、推荐配置方案

### 6.1 最小配置（推荐）

支持 OpenAI 兼容格式，覆盖 90% 的提供商：

```kotlin
data class MinimalProviderConfig(
    val id: String,
    val baseUrl: String,        // e.g. "https://api.openai.com/v1"
    val apiKey: String,
    val modelId: String,        // e.g. "gpt-4o"
    val endpointPath: String = "/chat/completions",  // 默认 OpenAI 路径
    val authHeaderType: AuthHeaderType = AuthHeaderType.BEARER
)

enum class AuthHeaderType {
    BEARER,      // Authorization: Bearer {token}
    API_KEY,     // api-key: {token}
    X_API_KEY,   // x-api-key: {token}
    NONE
}
```

### 6.2 完整配置

```kotlin
data class FullProviderConfig(
    val id: String,
    val displayName: String,
    val baseUrl: String,
    val apiKey: String,
    val modelId: String,
    
    // 端点配置
    val endpointPath: String,
    val endpointVersion: String? = null,  // e.g. "v1"
    
    // 认证配置
    val authHeaderType: AuthHeaderType,
    val authHeaderName: String = "Authorization",
    val requiredHeaders: Map<String, String> = emptyMap(),
    
    // 请求/响应格式
    val requestFormat: RequestFormat,
    val responseFormat: ResponseFormat,
    
    // 功能支持
    val supportsFunctionCalling: Boolean,
    val supportsVision: Boolean,
    val supportsStreaming: Boolean
)

enum class RequestFormat {
    OPENAI, ANTHROPIC, OLLAMA, GOOGLE, CUSTOM
}

enum class ResponseFormat {
    OPENAI, ANTHROPIC, OLLAMA, GOOGLE, CUSTOM
}
```

---

## 七、快速参考卡片

### 7.1 端点路径速查

```
OpenAI 兼容:     POST /v1/chat/completions
Anthropic:       POST /v1/messages
Ollama:          POST /api/chat
Google Gemini:   POST /v1/models/{model}:generateContent
Azure OpenAI:    POST /openai/deployments/{id}/chat/completions?api-version=xxx
Hugging Face:    POST /models/{model_id}
```

### 7.2 认证头速查

```
OpenAI/Zhipu/Mistral:  Authorization: Bearer {key}
Azure OpenAI:          api-key: {key}
Anthropic:             x-api-key: {key} + anthropic-version: 2023-06-01
Google:                Authorization: Bearer {oauth_token}
Ollama:                无
```

### 7.3 关键差异速查

```
System Prompt 位置:
- OpenAI: messages[{role: "system", ...}]
- Anthropic: 顶层 system 字段
- Google: system_instruction 字段

工具调用字段:
- OpenAI: tool_calls[]
- Anthropic: tool_use[]
- Google: functionCall

图片输入:
- OpenAI: messages[].content[{type: "image_url", image_url: {url}}]
- Anthropic: messages[].content[{type: "image", source: {...}}]
- Ollama: messages[].images[] (base64 数组)
- Google: contents[].parts[{inline_data: {...}}]
```

---

## 八、总结

### 8.1 端点格式分类

| 类别 | 代表 | 市场份额 | 推荐度 |
|------|------|----------|--------|
| OpenAI 兼容 | OpenAI, 智谱，Mistral, Azure | ~70% | ⭐⭐⭐⭐⭐ |
| Anthropic | Claude | ~15% | ⭐⭐⭐⭐ |
| 其他 | Google, Ollama, HF | ~15% | ⭐⭐⭐ |

### 8.2 实现建议

1. **优先支持 OpenAI 兼容格式** - 覆盖最广
2. **单独适配 Anthropic** - Claude 能力强
3. **Ollama 作为本地备选** - 离线场景
4. **使用适配层抽象差异** - 便于扩展

### 8.3 LangChain4j 的抽象

LangChain4j 已经处理了这些差异：

```kotlin
// 统一接口，内部处理格式转换
val openAiModel = OpenAiChatModel.builder()...build()
val anthropicModel = AnthropicChatModel.builder()...build()
val ollamaModel = OllamaChatModel.builder()...build()

// 都实现 ChatLanguageModel 接口
val model: ChatLanguageModel = openAiModel  // 或 anthropicModel 或 ollamaModel
val response = model.generate(messages)
```
