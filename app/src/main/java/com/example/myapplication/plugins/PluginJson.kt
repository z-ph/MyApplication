package com.example.myapplication.plugins

import com.example.myapplication.data.dto.AgentStateDto
import com.example.myapplication.data.dto.ApiConfigDto
import com.example.myapplication.data.dto.ChatMessageDto
import com.example.myapplication.data.dto.ChatSessionDto
import com.example.myapplication.data.dto.PermissionStatusDto
import com.example.myapplication.data.dto.TestConnectionDto
import com.getcapacitor.JSArray
import com.getcapacitor.JSObject
import org.json.JSONObject

/**
 * Capacitor JSObject helpers for bridge DTOs (camelCase per bridge-api.md).
 */
object PluginJson {

    fun session(dto: ChatSessionDto): JSObject = JSObject().apply {
        put("id", dto.id)
        put("title", dto.title)
        put("createdAt", dto.createdAt)
        put("updatedAt", dto.updatedAt)
    }

    fun sessionsEnvelope(sessions: List<ChatSessionDto>): JSObject = JSObject().apply {
        put("sessions", JSArray().also { arr ->
            sessions.forEach { arr.put(session(it)) }
        })
    }

    fun sessionEnvelope(dto: ChatSessionDto): JSObject = JSObject().apply {
        put("session", session(dto))
    }

    fun message(dto: ChatMessageDto): JSObject = JSObject().apply {
        put("id", dto.id)
        put("timestamp", dto.timestamp)
        put("type", dto.type)
        put("content", dto.content)
        putNullable("isSuccess", dto.isSuccess)
        putNullable("errorMessage", dto.errorMessage)
        putNullable("toolName", dto.toolName)
        if (dto.parameters != null) {
            put("parameters", JSONObject(dto.parameters))
        } else {
            put("parameters", JSONObject.NULL)
        }
        putNullable("result", dto.result)
        putNullable("imageBase64", dto.imageBase64)
        putNullable("isRunning", dto.isRunning)
    }

    fun messagesEnvelope(messages: List<ChatMessageDto>): JSObject = JSObject().apply {
        put("messages", JSArray().also { arr ->
            messages.forEach { arr.put(message(it)) }
        })
    }

    fun messagesChanged(sessionId: String, messages: List<ChatMessageDto>): JSObject =
        JSObject().apply {
            put("sessionId", sessionId)
            put("messages", JSArray().also { arr ->
                messages.forEach { arr.put(message(it)) }
            })
        }

    /** Bare AgentStateDto — no outer "state" wrapper (bridge-api §2.3 / §3.2). */
    fun agentState(dto: AgentStateDto): JSObject = JSObject().apply {
        put("state", dto.state)
        put("step", dto.step)
        put("action", dto.action)
        put("thinking", dto.thinking)
        putNullable("result", dto.result)
        putNullable("error", dto.error)
    }

    fun taskProgress(
        sessionId: String?,
        agentState: AgentStateDto,
        message: String? = null,
    ): JSObject = JSObject().apply {
        put("sessionId", sessionId)
        put("state", agentState.state)
        put("step", agentState.step)
        put("action", agentState.action)
        put("thinking", agentState.thinking)
        putNullable("result", agentState.result)
        putNullable("error", agentState.error)
        putNullable("message", message)
    }

    fun apiConfig(dto: ApiConfigDto): JSObject = JSObject().apply {
        put("id", dto.id)
        put("name", dto.name)
        put("provider", dto.provider)
        put("apiKeyMasked", dto.apiKeyMasked)
        put("baseUrl", dto.baseUrl)
        put("modelId", dto.modelId)
        put("isActive", dto.isActive)
    }

    fun configsEnvelope(configs: List<ApiConfigDto>): JSObject = JSObject().apply {
        put("configs", JSArray().also { arr ->
            configs.forEach { arr.put(apiConfig(it)) }
        })
    }

    fun configEnvelope(
        dto: ApiConfigDto,
        reconfigured: Boolean? = null,
    ): JSObject = JSObject().apply {
        put("config", apiConfig(dto))
        if (reconfigured != null) {
            put("reconfigured", reconfigured)
        }
    }

    fun reconfiguredEnvelope(reconfigured: Boolean): JSObject = JSObject().apply {
        put("reconfigured", reconfigured)
    }

    fun modelsEnvelope(models: List<String>): JSObject = JSObject().apply {
        put("models", JSArray().also { arr ->
            models.forEach { arr.put(it) }
        })
    }

    fun testConnection(dto: TestConnectionDto): JSObject = JSObject().apply {
        put("success", dto.success)
        put("message", dto.message)
        putNullable("details", dto.details)
    }

    fun providersEnvelope(providers: List<Map<String, String>>): JSObject = JSObject().apply {
        put(
            "providers",
            JSArray().also { arr ->
                providers.forEach { p ->
                    arr.put(
                        JSObject().apply {
                            p.forEach { (k, v) -> put(k, v) }
                        },
                    )
                }
            },
        )
    }

    fun permissionStatus(dto: PermissionStatusDto): JSObject = JSObject().apply {
        put("accessibility", dto.accessibility)
        put("overlay", dto.overlay)
        put("screenCapture", dto.screenCapture)
        put("appList", dto.appList)
        put("notification", dto.notification)
        put("apiConfigured", dto.apiConfigured)
        put("shizuku", dto.shizuku)
        put("allReady", dto.allReady)
    }

    private fun JSObject.putNullable(key: String, value: Any?) {
        if (value == null) {
            put(key, JSONObject.NULL)
        } else {
            put(key, value)
        }
    }
}
