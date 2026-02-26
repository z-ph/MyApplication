package com.example.myapplication.api.model

import com.google.gson.annotations.SerializedName

// API Request Models
data class ApiRequest(
    @SerializedName("model")
    val model: String = "glm-4v",

    @SerializedName("messages")
    val messages: List<Message>,

    @SerializedName("stream")
    val stream: Boolean = false,

    @SerializedName("temperature")
    val temperature: Float? = null,

    @SerializedName("top_p")
    val topP: Float? = null,

    @SerializedName("max_tokens")
    val maxTokens: Int? = null
)

data class Message(
    @SerializedName("role")
    val role: String,

    @SerializedName("content")
    val content: List<ContentItem>
)

data class ContentItem(
    @SerializedName("type")
    val type: String,

    @SerializedName("text")
    val text: String? = null,

    @SerializedName("image_url")
    val imageUrl: ImageUrl? = null
)

data class ImageUrl(
    @SerializedName("url")
    val url: String
)

// API Response Models
data class ApiResponse(
    @SerializedName("id")
    val id: String?,

    @SerializedName("created")
    val created: Long?,

    @SerializedName("model")
    val model: String?,

    @SerializedName("choices")
    val choices: List<Choice>?,

    @SerializedName("usage")
    val usage: Usage?,

    @SerializedName("error")
    val error: ApiError?
)

data class Choice(
    @SerializedName("index")
    val index: Int,

    @SerializedName("message")
    val message: ResponseMessage?,

    @SerializedName("finish_reason")
    val finishReason: String?
)

data class ResponseMessage(
    @SerializedName("role")
    val role: String?,

    @SerializedName("content")
    val content: String?,

    @SerializedName("tool_calls")
    val toolCalls: List<ToolCall>?
)

data class ToolCall(
    @SerializedName("id")
    val id: String?,

    @SerializedName("type")
    val type: String?,

    @SerializedName("function")
    val function: FunctionCall?
)

data class FunctionCall(
    @SerializedName("name")
    val name: String?,

    @SerializedName("arguments")
    val arguments: String?
)

data class Usage(
    @SerializedName("prompt_tokens")
    val promptTokens: Int?,

    @SerializedName("completion_tokens")
    val completionTokens: Int?,

    @SerializedName("total_tokens")
    val totalTokens: Int?
)

data class ApiError(
    @SerializedName("code")
    val code: String?,

    @SerializedName("message")
    val message: String?,

    @SerializedName("type")
    val type: String?
)

// Action Models (for parsing tool call arguments)
data class ActionArgument(
    @SerializedName("type")
    val type: String,

    @SerializedName("x")
    val x: Float? = null,

    @SerializedName("y")
    val y: Float? = null,

    @SerializedName("direction")
    val direction: String? = null,

    @SerializedName("distance")
    val distance: Int? = null,

    @SerializedName("text")
    val text: String? = null,

    @SerializedName("node_id")
    val nodeId: String? = null,

    @SerializedName("action")
    val action: String? = null
)

sealed class UiAction {
    data class Click(val x: Float, val y: Float) : UiAction()
    data class Swipe(val direction: SwipeDirection, val distance: Int = 500) : UiAction()
    data class InputText(val text: String) : UiAction()
    data class ClickNode(val nodeId: String) : UiAction()
    data class Navigate(val action: NavigationAction) : UiAction()
    data class Wait(val durationMs: Long) : UiAction()
}

enum class SwipeDirection {
    UP, DOWN, LEFT, RIGHT
}

enum class NavigationAction {
    BACK, HOME, RECENTS
}
