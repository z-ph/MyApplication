package com.example.myapplication.data.dto

/**
 * Bridge DTOs for H5 ↔ native (docs/bridge-api.md §2).
 * JSON field names are camelCase when serialized via Capacitor JSObject / Gson.
 */

data class ChatSessionDto(
    val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
)

/**
 * Flat message model mapped from sealed [com.example.myapplication.data.model.ChatMessage].
 * type: user | ai | toolCall | screenshot | status
 */
data class ChatMessageDto(
    val id: String,
    val timestamp: Long,
    val type: String,
    val content: String,
    val isSuccess: Boolean? = null,
    val errorMessage: String? = null,
    val toolName: String? = null,
    val parameters: Map<String, Any>? = null,
    val result: String? = null,
    val imageBase64: String? = null,
    val isRunning: Boolean? = null,
)

/**
 * Agent state for LingxiAgent.getState / stateChanged (bare object, no wrapper).
 * state: IDLE | READY | RUNNING | COMPLETED | ERROR | CANCELLED
 */
data class AgentStateDto(
    val state: String,
    val step: String = "",
    val action: String = "",
    val thinking: String = "",
    val result: String? = null,
    val error: String? = null,
)
