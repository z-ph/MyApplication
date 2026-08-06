package com.example.myapplication.data.mapper

import com.example.myapplication.agent.LangChainAgentEngine
import com.example.myapplication.data.dto.AgentStateDto
import com.example.myapplication.data.dto.ChatMessageDto
import com.example.myapplication.data.dto.ChatSessionDto
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
}
