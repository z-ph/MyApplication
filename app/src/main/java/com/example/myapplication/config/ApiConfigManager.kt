package com.example.myapplication.config

import android.content.Context
import android.content.SharedPreferences
import com.example.myapplication.utils.Logger

class ApiConfigManager(private val context: Context) {

    data class ProviderConfig(
        val providerId: String,
        val apiKey: String,
        val baseUrl: String,
        val modelId: String,
        val customEndpoint: String? = null,
        val enabled: Boolean = true
    )

    data class ValidationResult(
        val valid: Boolean,
        val message: String
    )

    private val prefs: SharedPreferences = context.getSharedPreferences("api_config", Context.MODE_PRIVATE)
    private val logger = Logger("ApiConfigManager")

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

    fun switchProvider(providerId: String): Boolean {
        val provider = ModelProvider.fromId(providerId)
        prefs.edit().putString("current_provider", providerId).apply()
        logger.d("切换提供商：$providerId")
        return true
    }

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

    fun clearAll() {
        prefs.edit().clear().apply()
        logger.d("清除所有配置")
    }
}
