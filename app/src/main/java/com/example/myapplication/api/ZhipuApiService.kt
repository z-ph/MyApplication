package com.example.myapplication.api

import com.example.myapplication.api.model.ApiRequest
import com.example.myapplication.api.model.ApiResponse
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Url

/**
 * AI API Service Interface
 * Supports custom API endpoints for different providers
 */
interface ZhipuApiService {

    /**
     * Analyze screen image and generate automation actions
     *
     * @param url Full API URL (e.g., https://open.bigmodel.cn/api/v4/chat/completions)
     * @param authorization Bearer token in format "Bearer {apiKey}"
     * @param request API request containing image and prompt
     * @return API response with generated actions or error
     */
    @POST
    suspend fun analyzeScreen(
        @Url url: String,
        @Header("Authorization") authorization: String,
        @Body request: ApiRequest
    ): ApiResponse

    /**
     * Chat with the AI model
     *
     * @param url Full API URL
     * @param authorization Bearer token in format "Bearer {apiKey}"
     * @param request API request containing messages
     * @return API response with AI response
     */
    @POST
    suspend fun chat(
        @Url url: String,
        @Header("Authorization") authorization: String,
        @Body request: ApiRequest
    ): ApiResponse
}
