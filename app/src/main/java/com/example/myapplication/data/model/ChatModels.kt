package com.example.myapplication.data.model

import com.example.myapplication.api.model.UiAction

/**
 * Chat session representing a conversation
 */
data class ChatSession(
    val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long
)

/**
 * Sealed class representing different message types
 */
sealed class ChatMessage {
    abstract val id: String
    abstract val timestamp: Long

    /**
     * User message - the instruction/command from user
     */
    data class UserMessage(
        override val id: String,
        override val timestamp: Long,
        val content: String,
        val attachedImageBase64: String? = null
    ) : ChatMessage()

    /**
     * AI response message
     */
    data class AiMessage(
        override val id: String,
        override val timestamp: Long,
        val content: String,
        val isSuccess: Boolean = true,
        val errorMessage: String? = null
    ) : ChatMessage()

    /**
     * Tool/API call message - records action execution
     */
    data class ToolCallMessage(
        override val id: String,
        override val timestamp: Long,
        val toolName: String,
        val parameters: Map<String, Any>,
        val result: String? = null,
        val isSuccess: Boolean = true
    ) : ChatMessage()

    /**
     * Screenshot message - captures screen state
     */
    data class ScreenshotMessage(
        override val id: String,
        override val timestamp: Long,
        val imageBase64: String,
        val description: String = ""
    ) : ChatMessage()

    /**
     * Status message - shows progress/status updates
     */
    data class StatusMessage(
        override val id: String,
        override val timestamp: Long,
        val status: String,
        val isRunning: Boolean = false
    ) : ChatMessage()
}

/**
 * Message type enum for database storage
 */
enum class MessageType {
    USER, AI, TOOL_CALL, SCREENSHOT, STATUS
}

/**
 * Helper function to convert UiAction to readable description
 */
fun UiAction.toDescription(): String {
    return when (this) {
        is UiAction.Click -> "点击 (${x.toInt()}, ${y.toInt()})"
        is UiAction.Swipe -> "滑动 ${direction.name} (${distance}px)"
        is UiAction.InputText -> "输入: ${if (text.length > 20) text.take(20) + "..." else text}"
        is UiAction.ClickNode -> "点击节点: $nodeId"
        is UiAction.Navigate -> "导航: ${action.name}"
        is UiAction.Wait -> "等待: ${durationMs}ms"
    }
}

/**
 * Helper function to get icon name for UiAction
 */
fun UiAction.toIconName(): String {
    return when (this) {
        is UiAction.Click -> "click"
        is UiAction.Swipe -> "swipe"
        is UiAction.InputText -> "input"
        is UiAction.ClickNode -> "click_node"
        is UiAction.Navigate -> "navigate"
        is UiAction.Wait -> "wait"
    }
}
