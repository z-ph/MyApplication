package com.example.myapplication.data.mapper

import com.example.myapplication.agent.LangChainAgentEngine
import com.example.myapplication.config.ModelProvider
import com.example.myapplication.data.dto.AgentStateDto
import com.example.myapplication.data.dto.ApiConfigDto
import com.example.myapplication.data.dto.ChatMessageDto
import com.example.myapplication.data.dto.ChatSessionDto
import com.example.myapplication.data.dto.PermissionStatusDto
import com.example.myapplication.data.local.entities.ApiConfigEntity
import com.example.myapplication.data.model.ChatMessage
import com.example.myapplication.data.model.ChatSession

/**
 * Maps domain models to bridge DTOs (docs/bridge-api.md §2).
 * Pure functions — unit-testable on JVM without Android framework.
 */
object BridgeDtoMapper {

    fun ChatSession.toDto(): ChatSessionDto = ChatSessionDto(
        id = id,
        title = title,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    fun List<ChatSession>.toSessionDtos(): List<ChatSessionDto> = map { it.toDto() }

    fun ChatMessage.toDto(): ChatMessageDto = when (this) {
        is ChatMessage.UserMessage -> ChatMessageDto(
            id = id,
            timestamp = timestamp,
            type = "user",
            content = content,
            imageBase64 = attachedImageBase64,
        )
        is ChatMessage.AiMessage -> ChatMessageDto(
            id = id,
            timestamp = timestamp,
            type = "ai",
            content = content,
            isSuccess = isSuccess,
            errorMessage = errorMessage,
        )
        is ChatMessage.ToolCallMessage -> ChatMessageDto(
            id = id,
            timestamp = timestamp,
            type = "toolCall",
            content = toolName,
            toolName = toolName,
            parameters = parameters,
            result = result,
            isSuccess = isSuccess,
        )
        is ChatMessage.ScreenshotMessage -> ChatMessageDto(
            id = id,
            timestamp = timestamp,
            type = "screenshot",
            content = description,
            imageBase64 = imageBase64,
        )
        is ChatMessage.StatusMessage -> ChatMessageDto(
            id = id,
            timestamp = timestamp,
            type = "status",
            content = status,
            isRunning = isRunning,
        )
    }

    fun List<ChatMessage>.toMessageDtos(): List<ChatMessageDto> = map { it.toDto() }

    /**
     * Maps native agent state to bridge DTO.
     * step/action/thinking are empty until native exposes them.
     * @param forceCancelled when true (cancel path), expose CANCELLED even if engine wrote IDLE.
     */
    fun LangChainAgentEngine.AgentState.toDto(
        forceCancelled: Boolean = false,
    ): AgentStateDto {
        val stateName = if (forceCancelled) {
            "CANCELLED"
        } else {
            state.name
        }
        return AgentStateDto(
            state = stateName,
            step = "",
            action = "",
            thinking = "",
            result = result,
            error = error,
        )
    }

    /**
     * Room entity → bridge ApiConfigDto with masked key (never full apiKey).
     * providerId (lowercase) → provider (UPPER_SNAKE enum name).
     */
    fun ApiConfigEntity.toDto(): ApiConfigDto = ApiConfigDto(
        id = id,
        name = name,
        provider = providerIdToBridge(providerId),
        apiKeyMasked = maskApiKey(apiKey),
        baseUrl = baseUrl,
        modelId = modelId,
        isActive = isActive,
    )

    fun List<ApiConfigEntity>.toApiConfigDtos(): List<ApiConfigDto> = map { it.toDto() }

    /** Lowercase Room / ModelProvider.id → bridge UPPER enum-style string. */
    fun providerIdToBridge(providerId: String): String {
        val provider = ModelProvider.entries.find { it.id.equals(providerId, ignoreCase = true) }
        return provider?.name ?: providerId.uppercase().replace('-', '_')
    }

    /**
     * Bridge provider string (OPENAI / openai / azure-openai) → ModelProvider.
     * @throws IllegalArgumentException if unknown
     */
    fun providerFromBridge(provider: String): ModelProvider {
        val raw = provider.trim()
        if (raw.isEmpty()) {
            throw IllegalArgumentException("provider is required")
        }
        ModelProvider.entries.find { it.name.equals(raw, ignoreCase = true) }?.let { return it }
        ModelProvider.entries.find { it.id.equals(raw, ignoreCase = true) }?.let { return it }
        // azure_openai / GOOGLE-VERTEX style
        val normalized = raw.lowercase().replace('_', '-')
        ModelProvider.entries.find { it.id.equals(normalized, ignoreCase = true) }?.let { return it }
        throw IllegalArgumentException("Unknown provider: $provider")
    }

    /**
     * Mask API key for list responses. Empty → empty; short → "***"; else prefix + "***".
     */
    fun maskApiKey(apiKey: String): String {
        if (apiKey.isEmpty()) return ""
        if (apiKey.length <= 4) return "***"
        val prefixLen = minOf(3, apiKey.length - 3)
        return apiKey.take(prefixLen) + "***"
    }

    fun permissionStatus(
        accessibility: Boolean,
        overlay: Boolean,
        screenCapture: Boolean,
        appList: Boolean,
        notification: Boolean,
        apiConfigured: Boolean,
        shizuku: Boolean,
    ): PermissionStatusDto {
        val allReady = accessibility && overlay && screenCapture && appList && apiConfigured
        return PermissionStatusDto(
            accessibility = accessibility,
            overlay = overlay,
            screenCapture = screenCapture,
            appList = appList,
            notification = notification,
            apiConfigured = apiConfigured,
            shizuku = shizuku,
            allReady = allReady,
        )
    }
}
