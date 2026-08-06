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

/**
 * API config for LingxiApiConfig.list / create / update / configsChanged.
 * List responses use [apiKeyMasked] only — never full key.
 * provider: bridge uppercase enum style (OPENAI, ZHIPU, …).
 */
data class ApiConfigDto(
    val id: String,
    val name: String,
    val provider: String,
    val apiKeyMasked: String,
    val baseUrl: String,
    val modelId: String,
    val isActive: Boolean,
)

/**
 * Permission status for LingxiPermission.getStatus / refresh / statusChanged.
 * allReady = required system gates + active API config present.
 */
data class PermissionStatusDto(
    val accessibility: Boolean,
    val overlay: Boolean,
    val screenCapture: Boolean,
    val appList: Boolean,
    val notification: Boolean,
    val apiConfigured: Boolean,
    val shizuku: Boolean,
    val allReady: Boolean,
)

/**
 * Result of LingxiApiConfig.testConnection.
 */
data class TestConnectionDto(
    val success: Boolean,
    val message: String,
    val details: String? = null,
)
