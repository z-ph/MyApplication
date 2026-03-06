package com.example.myapplication.network

import com.example.myapplication.utils.Logger
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger as KtorLogger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Provides a singleton Ktor HttpClient for custom HTTP operations
 * Used for ModelFetcher and other non-LangChain4j HTTP requests
 */
object HttpClientProvider {

    private val logger = Logger("HttpClientProvider")

    @Volatile
    private var client: HttpClient? = null

    /**
     * Get the shared Ktor HttpClient instance
     */
    fun getKtorClient(): HttpClient {
        return client ?: synchronized(this) {
            client ?: createClient().also { client = it }
        }
    }

    private fun createClient(): HttpClient {
        return HttpClient(Android) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    prettyPrint = false
                })
            }

            install(Logging) {
                logger = object : KtorLogger {
                    override fun log(message: String) {
                        this@HttpClientProvider.logger.d(message)
                    }
                }
                level = LogLevel.INFO
            }

            defaultRequest {
                contentType(ContentType.Application.Json)
            }

            engine {
                connectTimeout = 30_000
                socketTimeout = 30_000
            }
        }
    }

    /**
     * Close the HTTP client (call when app is terminating)
     */
    fun close() {
        client?.let {
            try {
                it.close()
                logger.d("Ktor client closed")
            } catch (e: Exception) {
                logger.e("Error closing Ktor client: ${e.message}")
            }
        }
        client = null
    }
}
