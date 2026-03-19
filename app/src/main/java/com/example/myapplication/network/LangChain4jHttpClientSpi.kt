package com.example.myapplication.network

import dev.langchain4j.http.client.HttpRequest
import dev.langchain4j.http.client.SuccessfulHttpResponse
import dev.langchain4j.http.client.sse.ServerSentEventListener
import dev.langchain4j.http.client.sse.ServerSentEventParser

/**
 * LangChain4j SPI entrypoint.
 * Keep this class thin and delegate real transport logic to the registry.
 */
class LangChain4jHttpClientSpi : dev.langchain4j.http.client.HttpClient {

    override fun execute(request: HttpRequest): SuccessfulHttpResponse {
        return LangChainHttpClientRegistry.get().execute(request)
    }

    override fun execute(
        request: HttpRequest,
        parser: ServerSentEventParser,
        listener: ServerSentEventListener
    ) {
        LangChainHttpClientRegistry.get().execute(request, parser, listener)
    }
}
