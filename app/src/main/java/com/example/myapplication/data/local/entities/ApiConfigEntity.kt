package com.example.myapplication.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for API configuration
 * Supports multiple API configurations with different providers
 */
@Entity(tableName = "api_configs")
data class ApiConfigEntity(
    @PrimaryKey
    val id: String,
    val name: String,              // Configuration name (e.g., "我的OpenAI")
    val providerId: String,        // Provider ID (zhipu, openai, deepseek, etc.)
    val apiKey: String,            // API key
    val baseUrl: String,           // Custom base URL (empty = use default)
    val modelId: String,           // Selected model ID
    val isActive: Boolean,         // Whether this is the active configuration
    val createdAt: Long,
    val updatedAt: Long
) {
    /**
     * Get a display-friendly description
     */
    fun getDisplayDescription(): String {
        return "$providerId - $modelId"
    }
}
