package com.example.myapplication.api

import com.example.myapplication.config.ModelProvider
import com.example.myapplication.network.HttpClientProvider
import com.example.myapplication.utils.Logger
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Result of fetching models
 */
data class ModelFetchResult(
    val isSuccess: Boolean,
    val models: List<ModelInfo> = emptyList(),
    val error: String? = null
)

/**
 * Model information
 */
data class ModelInfo(
    val id: String,
    val name: String,
    val ownedBy: String? = null
)

/**
 * Response structure for OpenAI-compatible /models endpoint
 */
@Serializable
data class OpenAIModelsResponse(
    val data: List<OpenAIModel> = emptyList()
)

@Serializable
data class OpenAIModel(
    val id: String,
    val `object`: String? = null,
    val created: Long? = null,
    val owned_by: String? = null
)

/**
 * Fetches available models from API providers using Ktor
 */
class ModelFetcher {
    private val logger = Logger("ModelFetcher")
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Fetch models from a provider
     */
    suspend fun fetchModels(
        provider: ModelProvider,
        apiKey: String,
        baseUrl: String
    ): ModelFetchResult = withContext(Dispatchers.IO) {
        try {
            val actualBaseUrl = baseUrl.ifEmpty { provider.defaultBaseUrl }

            when (provider.id) {
                ModelProvider.ZHIPU.id -> fetchZhipuModels()
                ModelProvider.QWEN.id -> fetchQwenModels()
                else -> fetchOpenAICompatibleModels(actualBaseUrl, apiKey)
            }
        } catch (e: Exception) {
            logger.e("Failed to fetch models: ${e.message}", e)
            ModelFetchResult(
                isSuccess = false,
                error = "获取模型列表失败：${e.message}"
            )
        }
    }

    /**
     * Fetch models from OpenAI-compatible API (OpenAI, DeepSeek, custom)
     */
    private suspend fun fetchOpenAICompatibleModels(baseUrl: String, apiKey: String): ModelFetchResult {
        return try {
            val cleanBaseUrl = baseUrl.trimEnd('/')
            val modelsUrl = "$cleanBaseUrl/models"

            logger.d("Fetching models from: $modelsUrl")

            val client = HttpClientProvider.getKtorClient()
            val response = client.get(modelsUrl) {
                header("Authorization", "Bearer $apiKey")
            }

            if (!response.status.value.toString().startsWith("2")) {
                return ModelFetchResult(
                    isSuccess = false,
                    error = "HTTP ${response.status.value}: ${response.status.description}"
                )
            }

            val responseBody = response.bodyAsText()
            val modelsResponse = json.decodeFromString<OpenAIModelsResponse>(responseBody)

            val models = modelsResponse.data.mapNotNull { model ->
                try {
                    ModelInfo(
                        id = model.id,
                        name = model.id,
                        ownedBy = model.owned_by
                    )
                } catch (e: Exception) {
                    null
                }
            }.sortedBy { it.id }

            logger.d("Fetched ${models.size} models")
            ModelFetchResult(isSuccess = true, models = models)
        } catch (e: Exception) {
            logger.e("OpenAI compatible fetch error: ${e.message}", e)
            ModelFetchResult(
                isSuccess = false,
                error = "获取模型失败: ${e.message}"
            )
        }
    }

    /**
     * Fetch models from Zhipu AI
     * Note: Zhipu doesn't have a /v1/models endpoint, return known models
     */
    private fun fetchZhipuModels(): ModelFetchResult {
        return try {
            val knownModels = listOf(
                ModelInfo("glm-4v", "GLM-4V (Vision)", "zhipu"),
                ModelInfo("glm-4v-plus", "GLM-4V Plus (Vision)", "zhipu"),
                ModelInfo("glm-4-plus", "GLM-4 Plus", "zhipu"),
                ModelInfo("glm-4-0520", "GLM-4 0520", "zhipu"),
                ModelInfo("glm-4-air", "GLM-4 Air", "zhipu"),
                ModelInfo("glm-4-airx", "GLM-4 AirX", "zhipu"),
                ModelInfo("glm-4-flash", "GLM-4 Flash", "zhipu"),
                ModelInfo("glm-4-long", "GLM-4 Long", "zhipu"),
                ModelInfo("glm-3-turbo", "GLM-3 Turbo", "zhipu")
            )
            ModelFetchResult(isSuccess = true, models = knownModels)
        } catch (e: Exception) {
            logger.e("Zhipu fetch error: ${e.message}", e)
            ModelFetchResult(
                isSuccess = false,
                error = "获取智谱模型失败: ${e.message}"
            )
        }
    }

    /**
     * Fetch models from Qwen (Tongyi Qianwen)
     * Note: Qwen doesn't have a standard models endpoint, return known models
     */
    private fun fetchQwenModels(): ModelFetchResult {
        return try {
            val knownModels = listOf(
                ModelInfo("qwen-vl-max", "Qwen VL Max (Vision)", "alibaba"),
                ModelInfo("qwen-vl-plus", "Qwen VL Plus (Vision)", "alibaba"),
                ModelInfo("qwen-vl-ocr", "Qwen VL OCR", "alibaba"),
                ModelInfo("qwen-max", "Qwen Max", "alibaba"),
                ModelInfo("qwen-max-longcontext", "Qwen Max Long Context", "alibaba"),
                ModelInfo("qwen-plus", "Qwen Plus", "alibaba"),
                ModelInfo("qwen-turbo", "Qwen Turbo", "alibaba"),
                ModelInfo("qwen-long", "Qwen Long", "alibaba")
            )
            ModelFetchResult(isSuccess = true, models = knownModels)
        } catch (e: Exception) {
            logger.e("Qwen fetch error: ${e.message}", e)
            ModelFetchResult(
                isSuccess = false,
                error = "获取通义千问模型失败: ${e.message}"
            )
        }
    }
}
