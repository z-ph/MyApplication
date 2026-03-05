package com.example.myapplication.data.mapper

import com.example.myapplication.data.local.entities.MessageEntity
import com.example.myapplication.data.local.entities.SessionEntity
import com.example.myapplication.data.model.ChatMessage
import com.example.myapplication.data.model.ChatSession
import com.example.myapplication.data.model.MessageType
import com.google.gson.Gson

/**
 * Data Mapper for Chat entities and domain models
 * Centralizes all conversion logic between Entity and Model layers
 */
object ChatMapper {

    private val gson = Gson()

    // Session mappings
    fun SessionEntity.toChatSession(): ChatSession = ChatSession(
        id = id,
        title = title,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    fun ChatSession.toSessionEntity(): SessionEntity = SessionEntity(
        id = id,
        title = title,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    // Message mappings
    fun MessageEntity.toChatMessage(): ChatMessage? {
        return try {
            when (type) {
                MessageType.USER.name -> {
                    val data = gson.fromJson(contentJson, UserMessageData::class.java)
                    ChatMessage.UserMessage(
                        id = id,
                        timestamp = timestamp,
                        content = data.content,
                        attachedImageBase64 = data.attachedImageBase64
                    )
                }
                MessageType.AI.name -> {
                    val data = gson.fromJson(contentJson, AiMessageData::class.java)
                    ChatMessage.AiMessage(
                        id = id,
                        timestamp = timestamp,
                        content = data.content,
                        isSuccess = data.isSuccess,
                        errorMessage = data.errorMessage
                    )
                }
                MessageType.TOOL_CALL.name -> {
                    val data = gson.fromJson(contentJson, ToolCallData::class.java)
                    ChatMessage.ToolCallMessage(
                        id = id,
                        timestamp = timestamp,
                        toolName = data.toolName,
                        parameters = data.parameters,
                        result = data.result,
                        isSuccess = data.isSuccess
                    )
                }
                MessageType.SCREENSHOT.name -> {
                    val data = gson.fromJson(contentJson, ScreenshotData::class.java)
                    ChatMessage.ScreenshotMessage(
                        id = id,
                        timestamp = timestamp,
                        imageBase64 = data.imageBase64,
                        description = data.description
                    )
                }
                MessageType.STATUS.name -> {
                    val data = gson.fromJson(contentJson, StatusData::class.java)
                    ChatMessage.StatusMessage(
                        id = id,
                        timestamp = timestamp,
                        status = data.status,
                        isRunning = data.isRunning
                    )
                }
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun ChatMessage.toMessageEntity(sessionId: String, sortOrder: Int): MessageEntity {
        val type: String
        val contentJson: String

        when (this) {
            is ChatMessage.UserMessage -> {
                type = MessageType.USER.name
                contentJson = gson.toJson(UserMessageData(content, attachedImageBase64))
            }
            is ChatMessage.AiMessage -> {
                type = MessageType.AI.name
                contentJson = gson.toJson(AiMessageData(content, isSuccess, errorMessage))
            }
            is ChatMessage.ToolCallMessage -> {
                type = MessageType.TOOL_CALL.name
                contentJson = gson.toJson(ToolCallData(toolName, parameters, result, isSuccess))
            }
            is ChatMessage.ScreenshotMessage -> {
                type = MessageType.SCREENSHOT.name
                contentJson = gson.toJson(ScreenshotData(imageBase64, description))
            }
            is ChatMessage.StatusMessage -> {
                type = MessageType.STATUS.name
                contentJson = gson.toJson(StatusData(status, isRunning))
            }
        }

        return MessageEntity(
            id = id,
            sessionId = sessionId,
            type = type,
            contentJson = contentJson,
            timestamp = timestamp,
            sortOrder = sortOrder
        )
    }

    // List mappings
    fun List<SessionEntity>.toChatSessions(): List<ChatSession> = map { it.toChatSession() }

    fun List<MessageEntity>.toChatMessages(): List<ChatMessage> = mapNotNull { it.toChatMessage() }

    // Data classes for JSON serialization
    private data class UserMessageData(
        val content: String,
        val attachedImageBase64: String?
    )

    private data class AiMessageData(
        val content: String,
        val isSuccess: Boolean,
        val errorMessage: String?
    )

    private data class ToolCallData(
        val toolName: String,
        val parameters: Map<String, Any>,
        val result: String?,
        val isSuccess: Boolean
    )

    private data class ScreenshotData(
        val imageBase64: String,
        val description: String
    )

    private data class StatusData(
        val status: String,
        val isRunning: Boolean
    )
}
