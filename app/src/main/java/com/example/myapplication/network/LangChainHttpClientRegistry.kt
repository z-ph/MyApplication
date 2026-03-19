package com.example.myapplication.network

/**
 * Holds the active LangChain HTTP transport implementation.
 * Defaults to Ktor and can be overridden in tests or future adapters.
 */
object LangChainHttpClientRegistry {

    @Volatile
    private var client: LangChainHttpClient = KtorHttpClient()

    fun get(): LangChainHttpClient = client

    fun register(client: LangChainHttpClient) {
        this.client.close()
        this.client = client
    }

    fun reset() {
        client.close()
        client = KtorHttpClient()
    }

    fun close() {
        client.close()
    }
}
