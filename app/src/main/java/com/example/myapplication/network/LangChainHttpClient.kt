package com.example.myapplication.network

import dev.langchain4j.http.client.HttpRequest
import dev.langchain4j.http.client.SuccessfulHttpResponse
import dev.langchain4j.http.client.sse.ServerSentEventListener
import dev.langchain4j.http.client.sse.ServerSentEventParser

/**
 * Internal abstraction for LangChain4j HTTP transport.
 * The SPI bridge delegates to this interface so transport implementations remain swappable.
 */
interface LangChainHttpClient {

    fun execute(request: HttpRequest): SuccessfulHttpResponse

    fun execute(
        request: HttpRequest,
        parser: ServerSentEventParser,
        listener: ServerSentEventListener
    )

    fun close() = Unit
}
