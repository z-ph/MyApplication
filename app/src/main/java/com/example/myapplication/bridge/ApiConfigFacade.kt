package com.example.myapplication.bridge

import com.example.myapplication.agent.LangChainAgentEngine
import com.example.myapplication.api.ModelFetcher
import com.example.myapplication.config.ModelProvider
import com.example.myapplication.data.dto.ApiConfigDto
import com.example.myapplication.data.dto.TestConnectionDto
import com.example.myapplication.data.mapper.BridgeDtoMapper.providerFromBridge
import com.example.myapplication.data.mapper.BridgeDtoMapper.toApiConfigDtos
import com.example.myapplication.data.mapper.BridgeDtoMapper.toDto
import com.example.myapplication.data.repository.ApiConfigRepository
import com.example.myapplication.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * API configuration façade for LingxiApiConfig (docs/bridge-api.md §3.3).
 * Logic migrated from ApiConfigViewModel; reconfigure agent on active changes.
 */
class ApiConfigFacade(
    private val repository: ApiConfigRepository,
    private val modelFetcher: ModelFetcher,
    private val agent: LangChainAgentEngine,
) {
    private val logger = Logger("ApiConfigFacade")

    val configsFlow: Flow<List<ApiConfigDto>> =
        repository.allConfigs.map { it.toApiConfigDtos() }

    suspend fun list(): List<ApiConfigDto> = withContext(Dispatchers.IO) {
        repository.allConfigs.first().toApiConfigDtos()
    }

    /**
     * @return config DTO and whether agent reconfigure was attempted/succeeded
     * (`reconfigured == null` means not attempted; `false` means attempted but failed).
     */
    suspend fun create(
        name: String,
        provider: String,
        apiKey: String,
        baseUrl: String,
        modelId: String,
    ): Pair<ApiConfigDto, Boolean?> {
        val modelProvider = providerFromBridge(provider)
        val result = repository.createConfig(
            name = name.ifBlank { modelProvider.displayName },
            provider = modelProvider,
            apiKey = apiKey,
            baseUrl = baseUrl,
            modelId = modelId,
        )
        val entity = result.getOrElse { throw it }
        val reconfigured = if (entity.isActive) reconfigureAgentQuietly() else null
        return entity.toDto() to reconfigured
    }

    /**
     * @return config DTO and reconfigure outcome (`null` if not attempted).
     */
    suspend fun update(
        id: String,
        name: String,
        provider: String,
        apiKey: String?,
        baseUrl: String,
        modelId: String,
    ): Pair<ApiConfigDto, Boolean?> {
        val existing = repository.getConfigById(id)
            ?: throw IllegalArgumentException("Config not found: $id")
        val modelProvider = providerFromBridge(provider)
        // Empty apiKey on update keeps existing secret (edit form UX).
        val keyToStore = if (apiKey.isNullOrBlank()) existing.apiKey else apiKey
        val result = repository.updateConfig(
            configId = id,
            name = name.ifBlank { existing.name },
            provider = modelProvider,
            apiKey = keyToStore,
            baseUrl = baseUrl,
            modelId = modelId,
        )
        result.getOrElse { throw it }
        val updated = repository.getConfigById(id)
            ?: throw IllegalStateException("Config missing after update: $id")
        val reconfigured =
            if (existing.isActive || updated.isActive) reconfigureAgentQuietly() else null
        return updated.toDto() to reconfigured
    }

    suspend fun delete(id: String): Boolean? {
        val existing = repository.getConfigById(id)
        val result = repository.deleteConfig(id)
        result.getOrElse { throw it }
        return if (existing?.isActive == true) reconfigureAgentQuietly() else null
    }

    /** @return whether agent reconfigure succeeded after setActive. */
    suspend fun setActive(id: String): Boolean {
        val result = repository.setActiveConfig(id)
        result.getOrElse { throw it }
        return reconfigureAgentQuietly()
    }

    /**
     * @param configId when [apiKey] is blank, load secret from Room for this id (edit-form UX).
     */
    suspend fun fetchModels(
        provider: String,
        apiKey: String,
        baseUrl: String,
        configId: String? = null,
    ): List<String> {
        val modelProvider = providerFromBridge(provider)
        val resolvedKey = resolveApiKey(apiKey, configId)
        val result = modelFetcher.fetchModels(modelProvider, resolvedKey, baseUrl)
        if (!result.isSuccess) {
            throw IllegalStateException(result.error ?: "Failed to fetch models")
        }
        return result.models.map { it.id }
    }

    /**
     * @param configId when [apiKey] is blank, load secret from Room for this id (edit-form UX).
     */
    suspend fun testConnection(
        provider: String,
        apiKey: String,
        baseUrl: String,
        modelId: String,
        configId: String? = null,
    ): TestConnectionDto {
        return try {
            val modelProvider = providerFromBridge(provider)
            val resolvedKey = resolveApiKey(apiKey, configId)
            val result = modelFetcher.fetchModels(modelProvider, resolvedKey, baseUrl)
            if (result.isSuccess) {
                TestConnectionDto(
                    success = true,
                    message = "连接成功！获取到 ${result.models.size} 个模型",
                    details = "Models: ${result.models.joinToString { it.name }}",
                )
            } else {
                TestConnectionDto(
                    success = false,
                    message = result.error ?: "连接失败",
                    details = null,
                )
            }
        } catch (e: Exception) {
            TestConnectionDto(
                success = false,
                message = "测试失败：${e.message}",
                details = null,
            )
        }
    }

    fun listProviders(): List<Map<String, String>> {
        return ModelProvider.getAllProviders().map { p ->
            mapOf(
                "id" to p.name,
                "displayName" to p.displayName,
                "defaultBaseUrl" to p.defaultBaseUrl,
                "defaultModel" to p.defaultModel,
            )
        }
    }

    /**
     * Non-blank [apiKey] wins. Blank + [configId] → Room secret.
     * Blank without usable stored key → [IllegalArgumentException].
     */
    suspend fun resolveApiKey(apiKey: String?, configId: String?): String {
        if (!apiKey.isNullOrBlank()) return apiKey
        if (!configId.isNullOrBlank()) {
            val stored = repository.getConfigById(configId)?.apiKey
            if (!stored.isNullOrBlank()) return stored
            throw IllegalArgumentException("No stored apiKey for configId=$configId")
        }
        throw IllegalArgumentException("apiKey is required when configId is not provided")
    }

    /**
     * @return true if reconfigure succeeded, false if failed (logged only).
     */
    private fun reconfigureAgentQuietly(): Boolean {
        return try {
            val initResult = agent.reconfigure()
            if (initResult.isSuccess) {
                logger.d("Agent reconfigured after API config change")
                true
            } else {
                logger.w(
                    "Agent reconfigure failed: ${initResult.exceptionOrNull()?.message}",
                )
                false
            }
        } catch (e: Exception) {
            logger.e("Agent reconfigure exception: ${e.message}", e)
            false
        }
    }
}
