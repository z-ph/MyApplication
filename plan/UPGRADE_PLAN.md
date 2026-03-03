# 手机 AI 助手框架升级方案

**版本**: v1.0  
**创建日期**: 2026-03-03  
**状态**: 待实施

---

## 一、现状分析

### 当前架构
```
┌─────────────────────────────────────────────────────────────┐
│                    UI Layer (Compose)                        │
└─────────────────────────────────────────────────────────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        │                     │                     │
        ▼                     ▼                     ▼
┌──────────────────┐ ┌──────────────────┐ ┌──────────────────┐
│   AgentEngine    │ │   TaskEngine     │ │   ZhipuApiClient │
│  (ReAct 状态机)   │ │  (遗留引擎)      │ │  (HTTP 客户端)     │
│  自研实现         │ │  待移除          │ │  智谱 AI 专用       │
└──────────────────┘ └──────────────────┘ └──────────────────┘
        │                     │                     │
        ▼                     ▼                     ▼
┌──────────────────┐ ┌──────────────────┐
│  ToolRegistry    │ │  AutoService     │
│  (25+ 工具)       │ │  (无障碍服务)     │
└──────────────────┘ └──────────────────┘
```

### 核心问题
| 问题 | 影响 | 优先级 |
|------|------|--------|
| 双引擎并存 | 维护成本高，代码重复 | 高 |
| 重复造轮子 | Agent 逻辑、上下文管理自己实现 | 高 |
| API 耦合 | ZhipuApiClient 与智谱绑定 | 中 |
| 缺少生态 | 无法利用成熟 LLM 框架 | 中 |

---

## 二、升级路线（两阶段）

### 📍 阶段一：LangChain4j 集成（核心，2-3 周）

**目标**：用成熟框架替换自研 Agent 逻辑，获得多模型支持和完整工具生态

#### 1.1 依赖配置

```gradle
// app/build.gradle
android {
    compileSdk = 34
    
    defaultConfig {
        minSdk = 26  // Android 8.0+
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
        coreLibraryDesugaringEnabled true
    }
}

dependencies {
    // === LangChain4j 核心 ===
    implementation("dev.langchain4j:langchain4j:1.0.0-beta1")
    implementation("dev.langchain4j:langchain4j-android:1.0.0-beta1")
    
    // === 模型提供商支持 ===
    // 智谱 AI
    implementation("dev.langchain4j:langchain4j-zhipu-ai:1.0.0-beta1")
    // OpenAI 兼容 API
    implementation("dev.langchain4j:langchain4j-open-ai:1.0.0-beta1")
    // Anthropic Claude
    implementation("dev.langchain4j:langchain4j-anthropic:1.0.0-beta1")
    // Ollama (本地部署)
    implementation("dev.langchain4j:langchain4j-ollama:1.0.0-beta1")
    // Google Vertex AI
    implementation("dev.langchain4j:langchain4j-vertex-ai:1.0.0-beta1")
    // Azure OpenAI
    implementation("dev.langchain4j:langchain4j-azure-open-ai:1.0.0-beta1")
    // Mistral AI
    implementation("dev.langchain4j:langchain4j-mistral-ai:1.0.0-beta1")
    // Hugging Face
    implementation("dev.langchain4j:langchain4j-hugging-face:1.0.0-beta1")
    
    // === 可选：向量数据库（长期记忆）===
    implementation("dev.langchain4j:langchain4j-easy-rag:1.0.0-beta1")
    
    // === 核心库 desugaring ===
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
}
```

#### 1.2 模型提供商配置系统

```kotlin
// config/ModelProviders.kt

/**
 * 支持的 AI 模型提供商
 */
enum class ModelProvider(
    val id: String,
    val displayName: String,
    val defaultBaseUrl: String,
    val defaultModel: String,
    val endpointFormat: String,  // 端点格式：{baseUrl}{endpointFormat}
    val authHeaderFormat: String // 认证头格式："Bearer {token}" 或 "Basic {token}"
) {
    ZHIPU(
        id = "zhipu",
        displayName = "智谱 AI",
        defaultBaseUrl = "https://open.bigmodel.cn/api/paas/v4",
        defaultModel = "glm-4",
        endpointFormat = "/chat/completions",
        authHeaderFormat = "Bearer {token}"
    ),
    
    OPENAI(
        id = "openai",
        displayName = "OpenAI",
        defaultBaseUrl = "https://api.openai.com/v1",
        defaultModel = "gpt-4o",
        endpointFormat = "/chat/completions",
        authHeaderFormat = "Bearer {token}"
    ),
    
    ANTHROPIC(
        id = "anthropic",
        displayName = "Anthropic (Claude)",
        defaultBaseUrl = "https://api.anthropic.com/v1",
        defaultModel = "claude-3-5-sonnet-20241022",
        endpointFormat = "/messages",
        authHeaderFormat = "Bearer {token}"
    ),
    
    OLLAMA(
        id = "ollama",
        displayName = "Ollama (自部署)",
        defaultBaseUrl = "http://localhost:11434",
        defaultModel = "llama3.2-vision",
        endpointFormat = "/api/chat",
        authHeaderFormat = ""  // 无需认证
    ),
    
    AZURE_OPENAI(
        id = "azure-openai",
        displayName = "Azure OpenAI",
        defaultBaseUrl = "https://{resource}.openai.azure.com/openai/deployments/{deployment}",
        defaultModel = "gpt-4o",
        endpointFormat = "/chat/completions?api-version=2024-02-15-preview",
        authHeaderFormat = "Bearer {token}"
    ),
    
    GOOGLE_VERTEX(
        id = "google-vertex",
        displayName = "Google Vertex AI",
        defaultBaseUrl = "https://{location}-aiplatform.googleapis.com/v1/projects/{project}/locations/{location}/publishers/google/models",
        defaultModel = "gemini-1.5-pro",
        endpointFormat = "/predict",
        authHeaderFormat = "Bearer {token}"
    ),
    
    MISTRAL(
        id = "mistral",
        displayName = "Mistral AI",
        defaultBaseUrl = "https://api.mistral.ai/v1",
        defaultModel = "mistral-large-latest",
        endpointFormat = "/chat/completions",
        authHeaderFormat = "Bearer {token}"
    ),
    
    HUGGING_FACE(
        id = "huggingface",
        displayName = "Hugging Face Inference",
        defaultBaseUrl = "https://api-inference.huggingface.co/models",
        defaultModel = "mistralai/Mixtral-8x7B-Instruct-v0.1",
        endpointFormat = "",
        authHeaderFormat = "Bearer {token}"
    ),
    
    CUSTOM(
        id = "custom",
        displayName = "自定义 API",
        defaultBaseUrl = "",
        defaultModel = "",
        endpointFormat = "/chat/completions",
        authHeaderFormat = "Bearer {token}"
    );
    
    /**
     * 构建完整的 API URL
     */
    fun buildApiUrl(baseUrl: String, customEndpoint: String? = null): String {
        val endpoint = customEndpoint ?: endpointFormat
        return if (baseUrl.endsWith("/")) {
            "${baseUrl}${endpoint.trimStart('/')}"
        } else {
            "$baseUrl$endpoint"
        }
    }
    
    /**
     * 构建认证头
     */
    fun buildAuthHeader(apiKey: String): Map<String, String> {
        return if (authHeaderFormat.isEmpty()) {
            emptyMap()
        } else {
            mapOf("Authorization" to authHeaderFormat.replace("{token}", apiKey))
        }
    }
    
    companion object {
        fun fromId(id: String): ModelProvider = entries.find { it.id == id } ?: ZHIPU
        
        fun getAllProviders(): List<ModelProvider> = entries.toList()
        
        /**
         * 获取支持 Function Calling 的提供商列表
         */
        fun getProvidersWithFunctionCalling(): List<ModelProvider> = listOf(
            ZHIPU, OPENAI, ANTHROPIC, AZURE_OPENAI, GOOGLE_VERTEX, MISTRAL
        )
    }
}
```

#### 1.3 配置管理

```kotlin
// config/ApiConfigManager.kt

/**
 * API 配置管理器
 * 支持多提供商配置和动态切换
 */
class ApiConfigManager(private val context: Context) {
    
    private val prefs = context.getSharedPreferences("api_config", Context.MODE_PRIVATE)
    private val logger = Logger("ApiConfigManager")
    
    data class ProviderConfig(
        val providerId: String,
        val apiKey: String,
        val baseUrl: String,
        val modelId: String,
        val customEndpoint: String? = null,
        val enabled: Boolean = true
    )
    
    /**
     * 保存提供商配置
     */
    fun saveProviderConfig(config: ProviderConfig) {
        prefs.edit().apply {
            putString("current_provider", config.providerId)
            putString("api_key_${config.providerId}", config.apiKey)
            putString("base_url_${config.providerId}", config.baseUrl)
            putString("model_id_${config.providerId}", config.modelId)
            config.customEndpoint?.let { putString("custom_endpoint_${config.providerId}", it) }
            putBoolean("provider_enabled_${config.providerId}", config.enabled)
        }.apply()
        
        logger.d("保存配置：provider=${config.providerId}, model=${config.modelId}")
    }
    
    /**
     * 获取当前提供商配置
     */
    fun getCurrentProviderConfig(): ProviderConfig? {
        val providerId = prefs.getString("current_provider", "zhipu") ?: return null
        
        val provider = ModelProvider.fromId(providerId)
        return ProviderConfig(
            providerId = providerId,
            apiKey = prefs.getString("api_key_$providerId", "").orEmpty(),
            baseUrl = prefs.getString("base_url_$providerId", provider.defaultBaseUrl).orEmpty(),
            modelId = prefs.getString("model_id_$providerId", provider.defaultModel).orEmpty(),
            customEndpoint = prefs.getString("custom_endpoint_$providerId", null)
        )
    }
    
    /**
     * 切换提供商
     */
    fun switchProvider(providerId: String): Boolean {
        val provider = ModelProvider.fromId(providerId)
        prefs.edit().putString("current_provider", providerId).apply()
        logger.d("切换提供商：$providerId")
        return true
    }
    
    /**
     * 列出所有已配置的提供商
     */
    fun getConfiguredProviders(): List<ProviderConfig> {
        return ModelProvider.getAllProviders().mapNotNull { provider ->
            val apiKey = prefs.getString("api_key_${provider.id}", "").orEmpty()
            if (apiKey.isNotEmpty()) {
                ProviderConfig(
                    providerId = provider.id,
                    apiKey = apiKey,
                    baseUrl = prefs.getString("base_url_${provider.id}", provider.defaultBaseUrl).orEmpty(),
                    modelId = prefs.getString("model_id_${provider.id}", provider.defaultModel).orEmpty(),
                    customEndpoint = prefs.getString("custom_endpoint_${provider.id}", null),
                    enabled = prefs.getBoolean("provider_enabled_${provider.id}", true)
                )
            } else null
        }
    }
    
    /**
     * 验证配置完整性
     */
    fun validateConfig(config: ProviderConfig): ValidationResult {
        val provider = ModelProvider.fromId(config.providerId)
        
        return when {
            config.apiKey.isBlank() -> ValidationResult(false, "API Key 不能为空")
            config.baseUrl.isBlank() -> ValidationResult(false, "Base URL 不能为空")
            config.modelId.isBlank() -> ValidationResult(false, "Model ID 不能为空")
            !config.baseUrl.startsWith("http") -> ValidationResult(false, "Base URL 必须以 http(s) 开头")
            else -> ValidationResult(true, "配置有效")
        }
    }
    
    data class ValidationResult(
        val valid: Boolean,
        val message: String
    )
}
```

#### 1.4 工具适配（AndroidTools）

```kotlin
// agent/AndroidTools.kt

package com.example.myapplication.agent

import android.content.Context
import android.view.accessibility.AccessibilityNodeInfo
import com.example.myapplication.accessibility.AutoService
import com.example.myapplication.api.model.SwipeDirection
import com.example.myapplication.screen.ScreenCapture
import com.example.myapplication.utils.Logger
import dev.langchain4j.agent.tool.P
import dev.langchain4j.agent.tool.Tool
import kotlinx.coroutines.runBlocking

/**
 * Android 自动化工具集
 * 使用 LangChain4j @Tool 注解
 */
class AndroidTools(
    private val appContext: Context,
    private val screenWidth: Int = 1080,
    private val screenHeight: Int = 2400,
    private val screenCapture: ScreenCapture = ScreenCapture.getInstance()
) {
    
    private val logger = Logger("AndroidTools")
    
    // ==================== 导航工具 ====================
    
    @Tool("按下返回键")
    fun back(): String {
        val service = AutoService.getInstance() ?: return "❌ 无障碍服务未启用"
        return if (service.pressBack()) "✅ 返回成功" else "❌ 返回失败"
    }
    
    @Tool("按下 Home 键，回到主屏幕")
    fun home(): String {
        val service = AutoService.getInstance() ?: return "❌ 无障碍服务未启用"
        return if (service.pressHome()) "✅ 回到主页" else "❌ 操作失败"
    }
    
    @Tool("打开最近任务列表")
    fun recents(): String {
        val service = AutoService.getInstance() ?: return "❌ 无障碍服务未启用"
        return if (service.pressRecents()) "✅ 打开最近任务" else "❌ 操作失败"
    }
    
    // ==================== 手势工具 ====================
    
    @Tool("点击屏幕指定坐标位置")
    fun click(
        @P("X 坐标 (0-$screenWidth)") x: Int,
        @P("Y 坐标 (0-$screenHeight)") y: Int
    ): String = runBlocking {
        val service = AutoService.getInstance() ?: return@runBlocking "❌ 无障碍服务未启用"
        
        val clampedX = x.coerceIn(0, screenWidth)
        val clampedY = y.coerceIn(0, screenHeight)
        
        logger.d("执行点击：($clampedX, $clampedY)")
        service.click(clampedX.toFloat(), clampedY.toFloat())
        "✅ 点击成功 ($clampedX, $clampedY)"
    }
    
    @Tool("长按屏幕指定坐标")
    fun longClick(
        @P("X 坐标 (0-$screenWidth)") x: Int,
        @P("Y 坐标 (0-$screenHeight)") y: Int,
        @P("长按时长 (毫秒)，默认 500") duration: Long = 500
    ): String {
        val service = AutoService.getInstance() ?: return "❌ 无障碍服务未启用"
        service.longClick(x.toFloat(), y.toFloat(), duration)
        return "✅ 长按成功 ($x, $y), 时长=${duration}ms"
    }
    
    @Tool("在屏幕上滑动")
    fun swipe(
        @P("滑动方向：up/down/left/right") direction: String,
        @P("滑动距离 (像素)，默认 500") distance: Int = 500
    ): String {
        val service = AutoService.getInstance() ?: return "❌ 无障碍服务未启用"
        
        val swipeDir = when (direction.lowercase()) {
            "up" -> SwipeDirection.UP
            "down" -> SwipeDirection.DOWN
            "left" -> SwipeDirection.LEFT
            "right" -> SwipeDirection.RIGHT
            else -> SwipeDirection.UP
        }
        
        service.swipe(swipeDir, distance, 300)
        return "✅ 滑动成功：$direction, 距离=$distance"
    }
    
    @Tool("输入文本到当前焦点输入框")
    fun type(
        @P("要输入的文本内容") text: String
    ): String {
        val service = AutoService.getInstance() ?: return "❌ 无障碍服务未启用"
        
        if (text.isEmpty()) return "❌ 输入文本不能为空"
        
        return if (service.inputText(text)) {
            val preview = text.take(30) + if (text.length > 30) "..." else ""
            "✅ 输入成功：$preview"
        } else {
            "❌ 输入失败"
        }
    }
    
    // ==================== UI 元素工具 ====================
    
    @Tool("根据文本查找屏幕上的控件，返回匹配信息")
    fun findNodesByText(
        @P("要查找的文本内容") text: String,
        @P("是否精确匹配，默认 false（模糊匹配）") exact: Boolean = false
    ): String {
        val service = AutoService.getInstance() ?: return "❌ 无障碍服务未启用"
        
        val nodes = service.findNodesByText(text, exact)
        if (nodes.isEmpty()) {
            return "❌ 未找到包含\"$text\"的控件"
        }
        
        val sb = StringBuilder("✅ 找到 ${nodes.size} 个匹配控件:\n")
        nodes.forEachIndexed { index, node ->
            val bounds = android.graphics.Rect()
            node.getBoundsInScreen(bounds)
            val nodeText = node.text?.toString() ?: ""
            sb.appendLine("${index + 1}. \"$nodeText\" at (${bounds.centerX()},${bounds.centerY()})")
        }
        
        return sb.toString()
    }
    
    @Tool("根据文本点击屏幕上的控件，优先使用此工具")
    fun clickByText(
        @P("要点击的控件文本") text: String,
        @P("是否精确匹配，默认 false") exact: Boolean = false
    ): String {
        val service = AutoService.getInstance() ?: return "❌ 无障碍服务未启用"
        
        val nodes = service.findNodesByText(text, exact)
        if (nodes.isEmpty()) {
            return "❌ 未找到包含\"$text\"的控件，尝试使用 find_nodes_by_text 查看可用控件"
        }
        
        for (node in nodes) {
            if (service.clickNode(node)) {
                val nodeText = node.text?.toString() ?: text
                return "✅ 成功点击 \"$nodeText\""
            }
        }
        
        return "❌ 找到控件但点击失败，可能需要改用坐标点击"
    }
    
    @Tool("滚动屏幕直到找到指定文本")
    fun scrollToText(
        @P("要查找的文本") text: String,
        @P("最大滑动次数，默认 10") maxSwipes: Int = 10,
        @P("滑动方向：up/down/auto，默认 auto") direction: String = "auto"
    ): String = runBlocking {
        val service = AutoService.getInstance() ?: return@runBlocking "❌ 无障碍服务未启用"
        
        // 先检查当前是否可见
        var nodes = service.findNodesByText(text, false)
        if (nodes.isNotEmpty()) {
            return@runBlocking "✅ 文本 \"$text\" 已在屏幕上可见"
        }
        
        val swipeDir = when (direction.lowercase()) {
            "down" -> SwipeDirection.DOWN
            else -> SwipeDirection.UP
        }
        
        for (i in 1..maxSwipes) {
            service.swipe(swipeDir, 800, 300)
            kotlinx.coroutines.delay(500)
            
            nodes = service.findNodesByText(text, false)
            if (nodes.isNotEmpty()) {
                return@runBlocking "✅ 滚动 $i 次后找到 \"$text\""
            }
        }
        
        return@runBlocking "❌ 滚动 $maxSwipes 次后仍未找到 \"$text\""
    }
    
    // ==================== 观察工具 ====================
    
    @Tool("获取当前屏幕的 UI 控件树信息")
    fun getUiHierarchy(): String {
        val service = AutoService.getInstance() ?: return "❌ 无障碍服务未启用"
        
        try {
            val root = service.getAccessibilityNodeInfo()
                ?: return "❌ 无法获取控件树根节点"
            
            val sb = StringBuilder("=== UI 控件树 ===\n\n")
            traverseNode(root, 0, sb)
            return sb.toString()
        } catch (e: Exception) {
            return "❌ 获取控件树失败：${e.message}"
        }
    }
    
    private fun traverseNode(
        node: AccessibilityNodeInfo,
        depth: Int,
        sb: StringBuilder
    ) {
        if (depth > 10) return
        
        val indent = "  ".repeat(depth)
        val text = node.text?.toString() ?: ""
        val desc = node.contentDescription?.toString() ?: ""
        val className = node.className?.toString()?.split(".")?.last() ?: "View"
        val bounds = android.graphics.Rect()
        node.getBoundsInScreen(bounds)
        
        val attrs = mutableListOf<String>()
        if (text.isNotEmpty()) attrs.add("text=\"$text\"")
        if (desc.isNotEmpty()) attrs.add("desc=\"$desc\"")
        attrs.add("pos=(${bounds.centerX()},${bounds.centerY()})")
        if (node.isClickable) attrs.add("clickable")
        if (node.isLongClickable) attrs.add("longClickable")
        if (node.isEditable) attrs.add("editable")
        if (node.isScrollable) attrs.add("scrollable")
        
        sb.appendLine("$indent$className [${attrs.joinToString(", ")}]")
        
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let {
                traverseNode(it, depth + 1, sb)
            }
        }
    }
    
    @Tool("捕获当前屏幕截图")
    fun captureScreen(): String = runBlocking {
        try {
            val bitmap = screenCapture.capture()
                ?: return@runBlocking "❌ 屏幕捕获失败"
            
            // 这里返回截图的 base64 或其他标识
            // 实际使用中，截图会直接附加到消息中
            "✅ 屏幕截图成功 (尺寸：${bitmap.width}x${bitmap.height})"
        } catch (e: Exception) {
            "❌ 屏幕截图失败：${e.message}"
        }
    }
    
    // ==================== 系统工具 ====================
    
    @Tool("打开指定的应用程序")
    fun openApp(
        @P("应用包名或应用名称") packageName: String
    ): String {
        try {
            val pm = appContext.packageManager
            var resolvedPackageName = packageName
            
            // 如果输入不是包名格式，尝试按名称查找
            if (!packageName.contains(".")) {
                val apps = pm.getInstalledApplications(0)
                val matchedApp = apps.find {
                    pm.getApplicationLabel(it).toString()
                        .contains(packageName, ignoreCase = true)
                }
                if (matchedApp != null) {
                    resolvedPackageName = matchedApp.packageName
                }
            }
            
            val intent = pm.getLaunchIntentForPackage(resolvedPackageName)
            if (intent != null) {
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                appContext.startActivity(intent)
                val label = pm.getApplicationLabel(
                    pm.getApplicationInfo(resolvedPackageName, 0)
                )
                return "✅ 打开应用成功：$label ($resolvedPackageName)"
            } else {
                return "❌ 找不到应用：$packageName"
            }
        } catch (e: Exception) {
            return "❌ 打开应用失败：${e.message}"
        }
    }
    
    @Tool("复制文本到剪贴板")
    fun copyToClipboard(
        @P("要复制的文本") text: String
    ): String {
        val service = AutoService.getInstance() ?: return "❌ 无障碍服务未启用"
        service.copyToClipboard(text)
        return "✅ 已复制到剪贴板：${text.take(30)}"
    }
    
    @Tool("等待指定时间，用于等待页面加载")
    fun wait(
        @P("等待时间 (毫秒)") ms: Long
    ): String = runBlocking {
        kotlinx.coroutines.delay(ms)
        return@runBlocking "✅ 等待 ${ms}ms"
    }
    
    @Tool("任务完成，结束执行并报告结果")
    fun finish(
        @P("任务完成总结") summary: String
    ): String {
        return "FINISH:$summary"
    }
    
    @Tool("回复用户消息")
    fun reply(
        @P("要发送给用户的消息") message: String
    ): String {
        return "REPLY:$message"
    }
}
```

#### 1.5 Agent 引擎重构

```kotlin
// agent/LangChainAgentEngine.kt

package com.example.myapplication.agent

import android.content.Context
import com.example.myapplication.config.ApiConfigManager
import com.example.myapplication.config.ModelProvider
import com.example.myapplication.utils.Logger
import dev.langchain4j.agent.tool.ToolExecutionRequest
import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.data.message.ChatMessage
import dev.langchain4j.memory.chat.MessageWindowChatMemory
import dev.langchain4j.model.chat.ChatLanguageModel
import dev.langchain4j.model.zhipu.ZhipuAiChatModel
import dev.langchain4j.model.openai.OpenAiChatModel
import dev.langchain4j.model.anthropic.AnthropicChatModel
import dev.langchain4j.model.ollama.OllamaChatModel
import dev.langchain4j.service.AiServices
import dev.langchain4j.service.SystemMessage
import dev.langchain4j.service.UserMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 基于 LangChain4j 的 Agent 引擎
 * 
 * 特性:
 * - 支持多模型提供商
 * - 自动 ReAct 循环
 * - Function Calling 开箱即用
 * - 内置上下文管理
 */
class LangChainAgentEngine(private val context: Context) {
    
    companion object {
        private const val TAG = "LangChainAgentEngine"
        private const val MAX_MESSAGES = 20
    }
    
    private val logger = Logger(TAG)
    private val configManager = ApiConfigManager(context)
    
    // Agent 状态
    private val _state = MutableStateFlow(AgentState())
    val state: StateFlow<AgentState> = _state.asStateFlow()
    
    // LangChain4j Assistant
    private var assistant: Assistant? = null
    
    // 当前配置
    private var currentConfig: ApiConfigManager.ProviderConfig? = null
    
    /**
     * 初始化 Agent
     */
    fun initialize(): Result<Unit> {
        return try {
            val config = configManager.getCurrentProviderConfig()
            if (config == null) {
                return Result.failure(Exception("未配置 API，请先在设置中配置"))
            }
            
            currentConfig = config
            
            // 创建 ChatModel
            val chatModel = createChatModel(config)
            
            // 创建工具集
            val tools = AndroidTools(context.applicationContext)
            
            // 创建记忆
            val chatMemory = MessageWindowChatMemory.withMaxMessages(MAX_MESSAGES)
            
            // 构建 Agent
            assistant = AiServices.builder(Assistant::class.java)
                .chatModel(chatModel)
                .tools(tools)
                .chatMemory(chatMemory)
                .build()
            
            _state.value = AgentState(state = AgentStateType.READY)
            logger.d("Agent 初始化成功：provider=${config.providerId}, model=${config.modelId}")
            Result.success(Unit)
        } catch (e: Exception) {
            logger.e("Agent 初始化失败：${e.message}", e)
            _state.value = AgentState(state = AgentStateType.ERROR, error = e.message)
            Result.failure(e)
        }
    }
    
    /**
     * 创建 ChatModel（支持多提供商）
     */
    private fun createChatModel(config: ApiConfigManager.ProviderConfig): ChatLanguageModel {
        val provider = ModelProvider.fromId(config.providerId)
        
        return when (provider.id) {
            "zhipu" -> ZhipuAiChatModel.builder()
                .apiKey(config.apiKey)
                .modelName(config.modelId)
                .build()
            
            "openai" -> OpenAiChatModel.builder()
                .baseUrl(config.baseUrl)
                .apiKey(config.apiKey)
                .modelName(config.modelId)
                .build()
            
            "anthropic" -> AnthropicChatModel.builder()
                .apiKey(config.apiKey)
                .modelName(config.modelId)
                .build()
            
            "ollama" -> OllamaChatModel.builder()
                .baseUrl(config.baseUrl)
                .modelName(config.modelId)
                .build()
            
            "custom" -> OpenAiChatModel.builder()
                .baseUrl(config.baseUrl)
                .apiKey(config.apiKey)
                .modelName(config.modelId)
                .build()
            
            else -> throw IllegalStateException("不支持的提供商：${provider.id}")
        }
    }
    
    /**
     * 执行任务
     */
    fun execute(task: String, callback: (AgentResult) -> Unit) {
        if (_state.value.state != AgentStateType.READY) {
            callback(AgentResult.error("Agent 未就绪，请先初始化"))
            return
        }
        
        val assistant = this.assistant ?: run {
            callback(AgentResult.error("Agent 未初始化"))
            return
        }
        
        _state.value = AgentState(state = AgentStateType.RUNNING)
        logger.d("开始执行任务：$task")
        
        try {
            val result = assistant.chat(task)
            
            // 检查是否是 finish/reply 特殊响应
            when {
                result.startsWith("FINISH:") -> {
                    val summary = result.substringAfter("FINISH:").trim()
                    _state.value = AgentState(state = AgentStateType.COMPLETED, result = summary)
                    callback(AgentResult.success(summary))
                }
                result.startsWith("REPLY:") -> {
                    val reply = result.substringAfter("REPLY:").trim()
                    callback(AgentResult.reply(reply))
                    // 继续保持 RUNNING 状态
                }
                else -> {
                    callback(AgentResult.success(result))
                }
            }
        } catch (e: Exception) {
            logger.e("任务执行失败：${e.message}", e)
            _state.value = AgentState(state = AgentStateType.ERROR, error = e.message)
            callback(AgentResult.error(e.message ?: "未知错误"))
        }
    }
    
    /**
     * 取消任务
     */
    fun cancel() {
        logger.d("取消任务")
        _state.value = AgentState(state = AgentStateType.IDLE)
        // TODO: 实现取消逻辑
    }
    
    /**
     * 清除记忆
     */
    fun clearMemory() {
        // 重新初始化以清除记忆
        initialize()
    }
    
    /**
     * 重新配置（切换提供商）
     */
    fun reconfigure(): Result<Unit> {
        return initialize()
    }
    
    // ==================== 状态类 ====================
    
    data class AgentState(
        val state: AgentStateType = AgentStateType.IDLE,
        val result: String? = null,
        val error: String? = null,
        val timestamp: Long = System.currentTimeMillis()
    )
    
    enum class AgentStateType {
        IDLE,       // 空闲
        READY,      // 就绪
        RUNNING,    // 执行中
        COMPLETED,  // 已完成
        ERROR,      // 错误
        CANCELLED   // 已取消
    }
    
    data class AgentResult(
        val success: Boolean,
        val message: String,
        val isReply: Boolean = false
    ) {
        companion object {
            fun success(message: String) = AgentResult(true, message)
            fun error(message: String) = AgentResult(false, message)
            fun reply(message: String) = AgentResult(true, message, isReply = true)
        }
    }
    
    // ==================== Assistant 接口 ====================
    
    interface Assistant {
        @SystemMessage("""
            你是一个手机自动化助手，可以通过分析屏幕来执行各种操作。
            
            ## 可用工具
            
            ### 导航
            - back(): 返回
            - home(): 回到主页
            - recents(): 最近任务
            
            ### 手势
            - click(x, y): 点击坐标
            - longClick(x, y, duration): 长按
            - swipe(direction, distance): 滑动 (up/down/left/right)
            - type(text): 输入文本
            
            ### UI 元素操作 (优先使用)
            - find_nodes_by_text(text, exact): 查找控件
            - click_by_text(text, exact): 根据文本点击控件
            - scroll_to_text(text, max_swipes, direction): 滚动查找文本
            - get_ui_hierarchy(): 获取控件树
            
            ### 观察
            - capture_screen(): 截图
            
            ### 系统
            - open_app(package_name): 打开应用
            - copy_to_clipboard(text): 复制到剪贴板
            
            ### 控制
            - wait(ms): 等待
            - finish(summary): 任务完成
            - reply(message): 回复用户
            
            ## 执行原则
            
            1. **优先使用 UI 元素工具**: click_by_text 比 click 更可靠
            2. **逐步思考**: 每步完成后观察结果再继续
            3. **错误处理**: 失败时尝试替代方案
            4. **适时完成**: 任务完成后调用 finish()
            
            ## 响应格式
            
            - 正常操作：直接调用相应工具
            - 需要与用户交流：调用 reply(message)
            - 任务完成：调用 finish(summary)
        """)
        fun chat(userMessage: String): String
    }
    
    companion object {
        @Volatile private var instance: LangChainAgentEngine? = null
        
        fun getInstance(context: Context): LangChainAgentEngine {
            return instance ?: synchronized(this) {
                instance ?: LangChainAgentEngine(context.applicationContext).also { instance = it }
            }
        }
    }
}
```

#### 1.6 迁移步骤

```
Week 1: 基础集成
├─ Day 1-2: 添加 LangChain4j 依赖
│   ├─ 配置 build.gradle
│   ├─ 启用 core library desugaring
│   └─ 验证构建成功
├─ Day 3-4: 创建 ModelProvider 和 ApiConfigManager
│   ├─ 定义所有提供商枚举
│   ├─ 实现配置管理
│   └─ 迁移现有 PreferencesManager 配置
└─ Day 5: 创建 AndroidTools 类
    ├─ 迁移 5 个核心工具
    └─ 验证工具调用

Week 2: 工具迁移 + 并行测试
├─ Day 1-3: 完成剩余 20+ 工具适配
│   ├─ get_ui_hierarchy, scroll_to_text
│   ├─ open_app, copy_to_clipboard
│   └─ 其他工具
├─ Day 4: 创建 LangChainAgentEngine
│   ├─ 实现多模型支持
│   └─ 验证 ReAct 循环
└─ Day 5: 双引擎并行测试
    ├─ 对比行为一致性
    └─ 性能基准测试

Week 3: 切换 + 清理
├─ Day 1-2: UI 层切换调用新引擎
│   ├─ 修改 ChatViewModel
│   └─ 验证 UI 交互
├─ Day 3: 移除旧 TaskEngine
├─ Day 4: 清理 ZhipuApiClient 重复逻辑
└─ Day 5: 回归测试 + 文档更新
```

---

### 📍 阶段二：云端 VLA 模型集成（可选，2-3 周）

**目标**：引入 UI-TARS 等视觉模型，减少 ReAct 轮次，提升响应速度

#### 2.1 混合架构

```
┌─────────────────────────────────────────────────────────────┐
│                   Hybrid Agent                              │
│  ┌─────────────────┐           ┌─────────────────┐         │
│  │  LangChain4j    │           │  VLA Client     │         │
│  │  (复杂任务规划)  │◀─────────▶│  (快速操作预测)  │         │
│  └────────┬────────┘           └────────┬────────┘         │
│           │                             │                   │
│           └──────────┬──────────────────┘                   │
│                      ▼                                      │
│           ┌─────────────────────┐                          │
│           │  Decision Router    │                          │
│           │  (置信度决策)        │                          │
│           └─────────────────────┘                          │
└─────────────────────────────────────────────────────────────┘
```

#### 2.2 VLA 客户端

```kotlin
// agent/VlaClient.kt

/**
 * 视觉 - 语言 - 动作模型客户端
 * 支持 UI-TARS、GLM-4V 等多模态模型
 */
class VlaClient(private val config: VlaConfig) {
    
    private val httpClient = OkHttpClient()
    private val gson = Gson()
    private val logger = Logger("VlaClient")
    
    data class VlaConfig(
        val apiUrl: String,
        val apiKey: String,
        val modelId: String,
        val timeoutSeconds: Long = 30
    )
    
    data class VlaRequest(
        val image: String,
        val instruction: String,
        val maxSteps: Int = 1
    )
    
    data class VlaResponse(
        val action: String,
        val coordinates: List<Float>?,
        val text: String?,
        val confidence: Float,
        val thought: String?
    )
    
    /**
     * 预测下一步动作
     */
    suspend fun predict(
        base64Image: String,
        instruction: String
    ): VlaResponse = withContext(Dispatchers.IO) {
        val request = VlaRequest(
            image = base64Image,
            instruction = instruction,
            maxSteps = 1
        )
        
        try {
            val response = httpClient.post(
                url = config.apiUrl,
                headers = mapOf(
                    "Authorization" to "Bearer ${config.apiKey}",
                    "Content-Type" to "application/json"
                ),
                body = gson.toJson(request)
            )
            
            gson.fromJson(response, VlaResponse::class.java)
        } catch (e: Exception) {
            logger.e("VLA 预测失败：${e.message}", e)
            VlaResponse(
                action = "error",
                coordinates = null,
                text = null,
                confidence = 0.0f,
                thought = e.message
            )
        }
    }
}
```

#### 2.3 决策路由

```kotlin
// agent/DecisionRouter.kt

/**
 * 决策路由器
 * 决定使用 VLA 快速路径还是 ReAct 规划路径
 */
class DecisionRouter(
    private val vlaClient: VlaClient,
    private val langChainAgent: LangChainAgentEngine,
    private val confidenceThreshold: Float = 0.85f
) {
    private val logger = Logger("DecisionRouter")
    
    /**
     * 执行任务，自动选择最佳路径
     */
    suspend fun execute(
        task: String,
        screenBase64: String
    ): AgentResult {
        // 1. 先尝试 VLA 快速预测
        val vlaResult = vlaClient.predict(screenBase64, task)
        
        logger.d("VLA 预测结果：action=${vlaResult.action}, confidence=${vlaResult.confidence}")
        
        return if (vlaResult.confidence >= confidenceThreshold) {
            // 高置信度：直接执行
            logger.d("VLA 高置信度，使用快速路径")
            executeVlaAction(vlaResult)
        } else {
            // 低置信度：启用 ReAct 规划
            logger.d("VLA 低置信度，使用 ReAct 路径")
            executeReAct(task)
        }
    }
    
    private suspend fun executeVlaAction(response: VlaResponse): AgentResult {
        return try {
            when (response.action) {
                "click" -> {
                    val (x, y) = response.coordinates!!
                    // 执行点击
                    AgentResult.success("VLA: 点击 ($x, $y)")
                }
                "swipe" -> {
                    // 执行滑动
                    AgentResult.success("VLA: 滑动")
                }
                "type" -> {
                    // 执行输入
                    AgentResult.success("VLA: 输入 ${response.text}")
                }
                else -> {
                    AgentResult.error("VLA: 未知动作 ${response.action}")
                }
            }
        } catch (e: Exception) {
            AgentResult.error("VLA 执行失败：${e.message}")
        }
    }
    
    private suspend fun executeReAct(task: String): AgentResult {
        // 使用 LangChain4j Agent 执行
        var result: AgentResult? = null
        langChainAgent.execute(task) { result = it }
        return result ?: AgentResult.error("ReAct 执行失败")
    }
}
```

---

## 三、配置示例

### 3.1 默认配置（JSON）

```json
{
  "providers": [
    {
      "id": "zhipu",
      "displayName": "智谱 AI",
      "defaultBaseUrl": "https://open.bigmodel.cn/api/paas/v4",
      "defaultModel": "glm-4",
      "endpointFormat": "/chat/completions",
      "authHeaderFormat": "Bearer {token}"
    },
    {
      "id": "openai",
      "displayName": "OpenAI",
      "defaultBaseUrl": "https://api.openai.com/v1",
      "defaultModel": "gpt-4o",
      "endpointFormat": "/chat/completions",
      "authHeaderFormat": "Bearer {token}"
    },
    {
      "id": "anthropic",
      "displayName": "Anthropic",
      "defaultBaseUrl": "https://api.anthropic.com/v1",
      "defaultModel": "claude-3-5-sonnet-20241022",
      "endpointFormat": "/messages",
      "authHeaderFormat": "Bearer {token}"
    },
    {
      "id": "ollama",
      "displayName": "Ollama",
      "defaultBaseUrl": "http://localhost:11434",
      "defaultModel": "llama3.2-vision",
      "endpointFormat": "/api/chat",
      "authHeaderFormat": ""
    },
    {
      "id": "mistral",
      "displayName": "Mistral AI",
      "defaultBaseUrl": "https://api.mistral.ai/v1",
      "defaultModel": "mistral-large-latest",
      "endpointFormat": "/chat/completions",
      "authHeaderFormat": "Bearer {token}"
    },
    {
      "id": "custom",
      "displayName": "自定义",
      "defaultBaseUrl": "",
      "defaultModel": "",
      "endpointFormat": "/chat/completions",
      "authHeaderFormat": "Bearer {token}"
    }
  ],
  "settings": {
    "maxMessages": 20,
    "confidenceThreshold": 0.85,
    "timeoutSeconds": 60
  }
}
```

---

## 四、风险与缓解

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| LangChain4j 兼容性 | 中 | 先 POC 验证，保留旧引擎降级 |
| 多模型切换复杂度 | 低 | 统一配置管理，UI 简化 |
| API 成本增加 | 中 | 设置使用限额，支持本地 Ollama |
| 迁移工作量 | 中 | 分阶段迁移，并行测试 |

---

## 五、验收标准

### 阶段一验收
- [ ] 所有 25+ 工具迁移完成
- [ ] 支持至少 3 个模型提供商
- [ ] 双引擎并行测试通过
- [ ] 旧 TaskEngine 移除
- [ ] 性能不低于原引擎

### 阶段二验收（可选）
- [ ] VLA 客户端集成完成
- [ ] 决策路由逻辑正确
- [ ] A/B 测试显示性能提升
- [ ] 降级策略有效

---

## 六、后续迭代

- [ ] 示范学习（Learning from Demonstration）
- [ ] 向量数据库长期记忆
- [ ] 多设备协同
- [ ] 插件系统

---

## 附录

### A. LangChain4j 资源
- 官网：https://langchain4j.dev
- GitHub: https://github.com/langchain4j/langchain4j
- 文档：https://docs.langchain4j.dev

### B. 模型提供商 API 文档
- 智谱 AI: https://open.bigmodel.cn/dev/api
- OpenAI: https://platform.openai.com/docs
- Anthropic: https://docs.anthropic.com/claude/docs
- Ollama: https://github.com/ollama/ollama
