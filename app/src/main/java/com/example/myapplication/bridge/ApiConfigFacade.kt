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

    suspend fun create(
        name: String,
        provider: String,
        apiKey: String,
        baseUrl: String,
        modelId: String,
    ): ApiConfigDto {
        val modelProvider = providerFromBridge(provider)
        val result = repository.createConfig(
            name = name.ifBlank { modelProvider.displayName },
            provider = modelProvider,
            apiKey = apiKey,
            baseUrl = baseUrl,
            modelId = modelId,
        )
        val entity = result.getOrElse { throw it }
        if (entity.isActive) {
            reconfigureAgentQuietly()
        }
        return entity.toDto()
    }

    suspend fun update(
        id: String,
        name: String,
        provider: String,
        apiKey: String?,
        baseUrl: String,
        modelId: String,
    ): ApiConfigDto {
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
        if (existing.isActive || updated.isActive) {
            reconfigureAgentQuietly()
        }
        return updated.toDto()
    }

    suspend fun delete(id: String) {
        val existing = repository.getConfigById(id)
        val result = repository.deleteConfig(id)
        result.getOrElse { throw it }
        if (existing?.isActive == true) {
            reconfigureAgentQuietly()
        }
    }

    suspend fun setActive(id: String) {
        val result = repository.setActiveConfig(id)
        result.getOrElse { throw it }
        reconfigureAgentQuietly()
    }

    suspend fun fetchModels(
        provider: String,
        apiKey: String,
        baseUrl: String,
    ): List<String> {
        val modelProvider = providerFromBridge(provider)
        val result = modelFetcher.fetchModels(modelProvider, apiKey, baseUrl)
        if (!result.isSuccess) {
            throw IllegalStateException(result.error ?: "Failed to fetch models")
        }
        return result.models.map { it.id }
    }

    suspend fun testConnection(
        provider: String,
        apiKey: String,
        baseUrl: String,
        modelId: String,
    ): TestConnectionDto {
        return try {
            val modelProvider = providerFromBridge(provider)
            val result = modelFetcher.fetchModels(modelProvider, apiKey, baseUrl)
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

    private fun reconfigureAgentQuietly() {
        try {
            val initResult = agent.reconfigure()
            if (initResult.isSuccess) {
                logger.d("Agent reconfigured after API config change")
            } else {
                logger.w(
                    "Agent reconfigure failed: ${initResult.exceptionOrNull()?.message}",
                )
            }
        } catch (e: Exception) {
            logger.e("Agent reconfigure exception: ${e.message}", e)
        }
    }
}
