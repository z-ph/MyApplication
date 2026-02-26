package com.example.myapplication.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * API Provider configuration
 */
data class ApiProvider(
    val id: String,
    val name: String,
    val defaultBaseUrl: String,
    val defaultModel: String,
    val endpointFormat: String  // e.g., "/v1/chat/completions" or "/api/paas/v4/chat/completions"
) {
    fun getFullUrl(baseUrl: String): String {
        val cleanBase = baseUrl.trimEnd('/')
        val cleanEndpoint = endpointFormat.trimStart('/')
        return "$cleanBase/$cleanEndpoint"
    }
}

/**
 * Predefined API providers
 */
object ApiProviders {
    val ZHIPU = ApiProvider(
        id = "zhipu",
        name = "智谱AI (Zhipu)",
        defaultBaseUrl = "https://open.bigmodel.cn",
        defaultModel = "glm-4v",
        endpointFormat = "/api/paas/v4/chat/completions"
    )

    val OPENAI = ApiProvider(
        id = "openai",
        name = "OpenAI",
        defaultBaseUrl = "https://api.openai.com",
        defaultModel = "gpt-4o",
        endpointFormat = "/v1/chat/completions"
    )

    val DEEPSEEK = ApiProvider(
        id = "deepseek",
        name = "DeepSeek",
        defaultBaseUrl = "https://api.deepseek.com",
        defaultModel = "deepseek-chat",
        endpointFormat = "/v1/chat/completions"
    )

    val QWEN = ApiProvider(
        id = "qwen",
        name = "通义千问 (Qwen)",
        defaultBaseUrl = "https://dashscope.aliyuncs.com",
        defaultModel = "qwen-vl-max",
        endpointFormat = "/api/v1/services/aigc/multimodal-generation/generation"
    )

    val CUSTOM = ApiProvider(
        id = "custom",
        name = "自定义 (Custom)",
        defaultBaseUrl = "",
        defaultModel = "",
        endpointFormat = "/v1/chat/completions"
    )

    val ALL = listOf(ZHIPU, OPENAI, DEEPSEEK, QWEN, CUSTOM)

    fun getById(id: String): ApiProvider {
        return ALL.find { it.id == id } ?: CUSTOM
    }
}

/**
 * Preferences Manager
 * Handles persistent storage of app configuration
 */
class PreferencesManager(context: Context) {

    companion object {
        private const val PREFS_NAME = "ai_automation_prefs"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_BASE_URL = "base_url"
        private const val KEY_MODEL_ID = "model_id"
        private const val KEY_PROVIDER_ID = "provider_id"

        @Volatile
        private var instance: PreferencesManager? = null

        fun getInstance(context: Context): PreferencesManager {
            return instance ?: synchronized(this) {
                instance ?: PreferencesManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // API Key
    var apiKey: String
        get() = prefs.getString(KEY_API_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_API_KEY, value).apply()

    // Base URL (without endpoint path)
    var baseUrl: String
        get() = prefs.getString(KEY_BASE_URL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_BASE_URL, value).apply()

    // Model ID
    var modelId: String
        get() = prefs.getString(KEY_MODEL_ID, "") ?: ""
        set(value) = prefs.edit().putString(KEY_MODEL_ID, value).apply()

    // Provider ID
    var providerId: String
        get() = prefs.getString(KEY_PROVIDER_ID, ApiProviders.ZHIPU.id) ?: ApiProviders.ZHIPU.id
        set(value) = prefs.edit().putString(KEY_PROVIDER_ID, value).apply()

    // Get current provider with settings
    fun getCurrentProvider(): ApiProvider {
        val provider = ApiProviders.getById(providerId)
        return provider.copy(
            defaultBaseUrl = baseUrl.ifEmpty { provider.defaultBaseUrl },
            defaultModel = modelId.ifEmpty { provider.defaultModel }
        )
    }

    // Get full API URL
    fun getFullApiUrl(): String {
        val provider = getCurrentProvider()
        val url = baseUrl.ifEmpty { provider.defaultBaseUrl }
        return provider.getFullUrl(url)
    }

    // Set provider with defaults
    fun setProvider(provider: ApiProvider) {
        providerId = provider.id
        if (baseUrl.isEmpty()) {
            baseUrl = provider.defaultBaseUrl
        }
        if (modelId.isEmpty()) {
            modelId = provider.defaultModel
        }
    }

    // Check if API is configured
    fun isApiConfigured(): Boolean {
        return apiKey.isNotEmpty()
    }

    // Clear all settings
    fun clearAll() {
        prefs.edit().clear().apply()
    }
}
