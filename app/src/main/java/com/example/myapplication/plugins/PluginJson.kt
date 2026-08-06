package com.example.myapplication.plugins

import com.example.myapplication.data.dto.AgentStateDto
import com.example.myapplication.data.dto.ChatMessageDto
import com.example.myapplication.data.dto.ChatSessionDto
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

    private fun JSObject.putNullable(key: String, value: Any?) {
        if (value == null) {
            put(key, JSONObject.NULL)
        } else {
            put(key, value)
        }
    }
}
