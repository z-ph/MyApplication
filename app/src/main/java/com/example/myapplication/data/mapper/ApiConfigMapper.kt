package com.example.myapplication.data.mapper

import com.example.myapplication.agent.langchain.ProviderConfig
import com.example.myapplication.config.ModelProvider
import com.example.myapplication.data.local.entities.ApiConfigEntity

/**
 * Data Mapper for API Configuration entities
 * Centralizes all conversion logic for API configuration
 */
object ApiConfigMapper {

    /**
     * Convert ApiConfigEntity to ProviderConfig for LangChain4j ModelFactory
     */
    fun ApiConfigEntity.toProviderConfig(): ProviderConfig = ProviderConfig(
        providerId = providerId,
        apiKey = apiKey,
        baseUrl = baseUrl,
        modelId = modelId
    )

    /**
     * Convert ApiConfigEntity to a display-friendly format
     */
    fun ApiConfigEntity.toDisplayString(): String {
        val provider = ModelProvider.fromId(providerId)
        return "$name (${provider.displayName} - $modelId)"
    }

    /**
     * Check if the configuration is valid
     */
    fun ApiConfigEntity.isValid(): Boolean {
        return apiKey.isNotBlank() &&
                baseUrl.isNotBlank() &&
                modelId.isNotBlank() &&
                providerId.isNotBlank()
    }

    /**
     * Get ModelProvider from ApiConfigEntity
     */
    fun ApiConfigEntity.toModelProvider(): ModelProvider {
        return ModelProvider.fromId(providerId)
    }

    /**
     * Create a new ApiConfigEntity with defaults from provider
     */
    fun createConfig(
        id: String,
        name: String,
        provider: ModelProvider,
        apiKey: String,
        baseUrl: String = "",
        modelId: String = "",
        isActive: Boolean = false
    ): ApiConfigEntity {
        val now = System.currentTimeMillis()
        return ApiConfigEntity(
            id = id,
            name = name,
            providerId = provider.id,
            apiKey = apiKey,
            baseUrl = baseUrl.ifEmpty { provider.defaultBaseUrl },
            modelId = modelId.ifEmpty { provider.defaultModel },
            isActive = isActive,
            createdAt = now,
            updatedAt = now
        )
    }

    /**
     * Update an existing ApiConfigEntity
     */
    fun ApiConfigEntity.update(
        name: String? = null,
        provider: ModelProvider? = null,
        apiKey: String? = null,
        baseUrl: String? = null,
        modelId: String? = null
    ): ApiConfigEntity {
        val newProvider = provider ?: ModelProvider.fromId(providerId)
        return copy(
            name = name ?: this.name,
            providerId = newProvider.id,
            apiKey = apiKey ?: this.apiKey,
            baseUrl = (baseUrl?.ifEmpty { null } ?: this.baseUrl).ifEmpty { newProvider.defaultBaseUrl },
            modelId = (modelId?.ifEmpty { null } ?: this.modelId).ifEmpty { newProvider.defaultModel },
            updatedAt = System.currentTimeMillis()
        )
    }
}
