package com.example.myapplication.network

import androidx.compose.runtime.mutableStateListOf
import com.example.myapplication.utils.Logger
import java.util.concurrent.ConcurrentHashMap

private const val TAG = "NetworkMonitor"

/**
 * Network monitor that captures HTTP requests
 * Works with KtorHttpClient for LangChain4j requests
 * Ktor requests are logged via HttpClientProvider's Logging plugin
 */
class NetworkMonitor private constructor() {

    companion object {
        @Volatile
        private var instance: NetworkMonitor? = null

        fun getInstance(): NetworkMonitor {
            return instance ?: synchronized(this) {
                instance ?: NetworkMonitor().also { instance = it }
            }
        }
    }

    private val logger = Logger(TAG)
    private val _requests = mutableStateListOf<NetworkRequest>()
    private val requestMap = ConcurrentHashMap<String, NetworkRequest>()

    val requests: List<NetworkRequest> get() = _requests.toList()

    fun addRequest(request: NetworkRequest) {
        requestMap[request.id] = request
        _requests.add(0, request)

        // Keep only last 100 requests to avoid memory issues
        if (_requests.size > 100) {
            val removed = _requests.removeAt(_requests.size - 1)
            requestMap.remove(removed.id)
        }

        logger.d("Added request: ${request.method} ${request.url} - Status: ${request.response?.status}")
    }

    fun updateRequest(id: String, update: (NetworkRequest) -> NetworkRequest) {
        requestMap[id]?.let { existing ->
            val updated = update(existing)
            requestMap[id] = updated

            val index = _requests.indexOfFirst { it.id == id }
            if (index >= 0) {
                _requests[index] = updated
            }
        }
    }

    fun clear() {
        _requests.clear()
        requestMap.clear()
        logger.d("Network monitor cleared")
    }

    fun generateRequestId(): String {
        return System.currentTimeMillis().toString() + "_" + (1000..9999).random()
    }
}

data class NetworkRequest(
    val id: String,
    val url: String,
    val method: String,
    val timestamp: Long = System.currentTimeMillis(),
    val duration: Long = 0,
    val requestHeaders: Map<String, String> = emptyMap(),
    val requestBody: String? = null,
    val requestSize: Long = 0,
    val response: NetworkResponse? = null,
    val responseSize: Long = 0,
    val error: String? = null
) {
    val status: Int get() = response?.status ?: -1
    val statusText: String get() = response?.statusText ?: error ?: "Pending"

    fun getStatusColor(): String {
        return when {
            status == -1 -> "error"
            status in 200..299 -> "success"
            status in 300..399 -> "redirect"
            status in 400..499 -> "client_error"
            status >= 500 -> "server_error"
            else -> "pending"
        }
    }
}

data class NetworkResponse(
    val status: Int,
    val statusText: String,
    val headers: Map<String, String>,
    val body: String?,
    val contentType: String? = null
)
