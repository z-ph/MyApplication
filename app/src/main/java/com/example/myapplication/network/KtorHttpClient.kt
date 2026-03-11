package com.example.myapplication.network

import com.example.myapplication.utils.Logger
import dev.langchain4j.exception.HttpException
import dev.langchain4j.http.client.HttpRequest
import dev.langchain4j.http.client.SuccessfulHttpResponse
import dev.langchain4j.http.client.sse.ServerSentEventParser
import dev.langchain4j.http.client.sse.ServerSentEventListener
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import io.ktor.utils.io.jvm.javaio.toInputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * LangChain4j HttpClient implementation using Ktor
 * This replaces the default JdkHttpClient with Ktor for better Android compatibility
 */
class KtorHttpClient : dev.langchain4j.http.client.HttpClient {

    companion object {
        private const val TAG = "KtorHttpClient"

        @Volatile
        private var instance: KtorHttpClient? = null

        fun getInstance(): KtorHttpClient {
            return instance ?: synchronized(this) {
                instance ?: KtorHttpClient().also { instance = it }
            }
        }
    }

    private val logger = Logger(TAG)
    private val ktorClient = HttpClientProvider.getKtorClient()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun execute(request: HttpRequest): SuccessfulHttpResponse {
        logger.d("Executing request: ${request.method()} ${request.url()}")

        val networkMonitor = NetworkMonitor.getInstance()
        val requestId = generateRequestId()
        val startTime = System.currentTimeMillis()

        // Record request for monitoring
        val networkRequest = NetworkRequest(
            id = requestId,
            url = request.url(),
            method = request.method().name,
            requestHeaders = request.headers().mapValues { it.value.firstOrNull() ?: "" },
            requestBody = request.body(),
            requestSize = request.body()?.length?.toLong() ?: 0
        )

        try {
            // Execute synchronous request using runBlocking
            val response = runBlocking {
                executeKtorRequest(request)
            }

            val endTime = System.currentTimeMillis()
            val responseBody = runBlocking { response.bodyAsText() }

            // Record response for monitoring
            networkMonitor.addRequest(
                networkRequest.copy(
                    duration = endTime - startTime,
                    response = NetworkResponse(
                        status = response.status.value,
                        statusText = response.status.description,
                        headers = response.headers.entries().associate { it.key to it.value.first() },
                        body = responseBody,
                        contentType = response.headers["Content-Type"]
                    ),
                    timestamp = System.currentTimeMillis(),
                    responseSize = responseBody.length.toLong()
                )
            )

            // Check for HTTP errors
            if (response.status.value !in 200..299) {
                throw HttpException(response.status.value, "${response.status.value} ${response.status.description}: $responseBody")
            }

            return SuccessfulHttpResponse.builder()
                .statusCode(response.status.value)
                .headers(response.headers.entries().associate { it.key to it.value })
                .body(responseBody)
                .build()

        } catch (e: HttpException) {
            throw e
        } catch (e: Exception) {
            val endTime = System.currentTimeMillis()
            logger.e("Request failed: ${e.message}", e)

            // Record error for monitoring
            networkMonitor.addRequest(
                networkRequest.copy(
                    duration = endTime - startTime,
                    response = NetworkResponse(
                        status = -1,
                        statusText = "Network Error: ${e.message}",
                        headers = emptyMap(),
                        body = null
                    ),
                    timestamp = System.currentTimeMillis(),
                    error = e.message
                )
            )

            throw RuntimeException("Failed to execute request: ${e.message}", e)
        }
    }

    override fun execute(request: HttpRequest, parser: ServerSentEventParser, listener: ServerSentEventListener) {
        logger.d("Executing SSE request: ${request.method()} ${request.url()}")

        val networkMonitor = NetworkMonitor.getInstance()
        val requestId = generateRequestId()
        val startTime = System.currentTimeMillis()

        // Record request for monitoring
        val networkRequest = NetworkRequest(
            id = requestId,
            url = request.url(),
            method = request.method().name,
            requestHeaders = request.headers().mapValues { it.value.firstOrNull() ?: "" },
            requestBody = request.body(),
            requestSize = request.body()?.length?.toLong() ?: 0
        )

        scope.launch {
            try {
                val response = executeKtorRequest(request)

                // Record successful start
                networkMonitor.addRequest(
                    networkRequest.copy(
                        duration = System.currentTimeMillis() - startTime,
                        response = NetworkResponse(
                            status = response.status.value,
                            statusText = response.status.description,
                            headers = response.headers.entries().associate { it.key to it.value.first() },
                            body = "[SSE Stream]",
                            contentType = response.headers["Content-Type"]
                        ),
                        timestamp = System.currentTimeMillis()
                    )
                )

                if (response.status.value !in 200..299) {
                    val errorBody = response.bodyAsText()
                    listener.onError(HttpException(response.status.value, "${response.status.value} ${response.status.description}: $errorBody"))
                    return@launch
                }

                // Use the parser to process the SSE stream
                val inputStream = response.bodyAsChannel().toInputStream()
                parser.parse(inputStream, listener)

            } catch (e: Exception) {
                logger.e("SSE request failed: ${e.message}", e)

                networkMonitor.addRequest(
                    networkRequest.copy(
                        duration = System.currentTimeMillis() - startTime,
                        response = NetworkResponse(
                            status = -1,
                            statusText = "SSE Error: ${e.message}",
                            headers = emptyMap(),
                            body = null
                        ),
                        timestamp = System.currentTimeMillis(),
                        error = e.message
                    )
                )

                listener.onError(e)
            }
        }
    }

    private suspend fun executeKtorRequest(request: HttpRequest): io.ktor.client.statement.HttpResponse {
        return ktorClient.request(request.url()) {
            // LangChain4j 1.x HttpMethod enum only has GET, POST, DELETE
            method = when (request.method()) {
                dev.langchain4j.http.client.HttpMethod.GET -> HttpMethod.Get
                dev.langchain4j.http.client.HttpMethod.POST -> HttpMethod.Post
                dev.langchain4j.http.client.HttpMethod.DELETE -> HttpMethod.Delete
            }

            // Add headers
            request.headers().forEach { (name, values) ->
                values.forEach { value ->
                    header(name, value)
                }
            }

            // Add body if present
            request.body()?.let { body ->
                contentType(ContentType.Application.Json)
                setBody(body)
            }
        }
    }

    private fun generateRequestId(): String {
        return System.currentTimeMillis().toString() + "_" + (1000..9999).random()
    }

    fun close() {
        scope.cancel()
        logger.d("KtorHttpClient closed")
    }
}
