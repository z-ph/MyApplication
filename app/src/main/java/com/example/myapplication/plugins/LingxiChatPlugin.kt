package com.example.myapplication.plugins

import com.example.myapplication.bridge.ChatFacade
import com.example.myapplication.di.ServiceLocator
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * Capacitor plugin: chat sessions / messages / send / cancel.
 * Contract: docs/bridge-api.md §3.1 LingxiChat
 */
@CapacitorPlugin(name = "LingxiChat")
class LingxiChatPlugin : Plugin() {

    private val pluginScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var sessionsJob: Job? = null
    private var messagesJob: Job? = null
    private var progressJob: Job? = null

    private val facade: ChatFacade
        get() = ServiceLocator.getChatFacade(context.applicationContext)

    override fun load() {
        super.load()
        facade.ensureInitialized()
        startEventPipes()
    }

    override fun handleOnDestroy() {
        sessionsJob?.cancel()
        messagesJob?.cancel()
        progressJob?.cancel()
        super.handleOnDestroy()
    }

    private fun startEventPipes() {
        sessionsJob?.cancel()
        sessionsJob = pluginScope.launch {
            facade.sessions.collectLatest { sessions ->
                notifyListeners("sessionsChanged", PluginJson.sessionsEnvelope(sessions))
            }
        }

        messagesJob?.cancel()
        messagesJob = pluginScope.launch {
            combine(
                facade.currentSessionId,
                facade.currentMessages,
            ) { sessionId, messages ->
                sessionId to messages
            }.collectLatest { (sessionId, messages) ->
                if (sessionId != null) {
                    notifyListeners(
                        "messagesChanged",
                        PluginJson.messagesChanged(sessionId, messages),
                    )
                }
            }
        }

        progressJob?.cancel()
        progressJob = pluginScope.launch {
            val agentFacade = ServiceLocator.getAgentFacade(context.applicationContext)
            agentFacade.stateFlow()
                .distinctUntilChanged()
                .collectLatest { state ->
                    notifyListeners(
                        "taskProgress",
                        PluginJson.taskProgress(facade.currentSessionId.value, state),
                    )
                }
        }
    }

    @PluginMethod
    fun listSessions(call: PluginCall) {
        pluginScope.launch {
            try {
                val sessions = facade.listSessions()
                call.resolve(PluginJson.sessionsEnvelope(sessions))
            } catch (e: Exception) {
                call.reject("listSessions failed: ${e.message}", e)
            }
        }
    }

    @PluginMethod
    fun createSession(call: PluginCall) {
        val title = call.getString("title")
        pluginScope.launch {
            try {
                val session = facade.createSession(title)
                call.resolve(PluginJson.sessionEnvelope(session))
            } catch (e: Exception) {
                call.reject("createSession failed: ${e.message}", e)
            }
        }
    }

    @PluginMethod
    fun selectSession(call: PluginCall) {
        val sessionId = call.getString("sessionId")
        if (sessionId.isNullOrBlank()) {
            call.reject("sessionId is required")
            return
        }
        try {
            facade.selectSession(sessionId)
            call.resolve()
        } catch (e: Exception) {
            call.reject("selectSession failed: ${e.message}", e)
        }
    }

    @PluginMethod
    fun deleteSession(call: PluginCall) {
        val sessionId = call.getString("sessionId")
        if (sessionId.isNullOrBlank()) {
            call.reject("sessionId is required")
            return
        }
        pluginScope.launch {
            try {
                facade.deleteSession(sessionId)
                call.resolve()
            } catch (e: Exception) {
                call.reject("deleteSession failed: ${e.message}", e)
            }
        }
    }

    @PluginMethod
    fun listMessages(call: PluginCall) {
        val sessionId = call.getString("sessionId")
        if (sessionId.isNullOrBlank()) {
            call.reject("sessionId is required")
            return
        }
        pluginScope.launch {
            try {
                val messages = facade.listMessages(sessionId)
                call.resolve(PluginJson.messagesEnvelope(messages))
            } catch (e: Exception) {
                call.reject("listMessages failed: ${e.message}", e)
            }
        }
    }

    @PluginMethod
    fun sendMessage(call: PluginCall) {
        val content = call.getString("content")
        if (content.isNullOrBlank()) {
            call.reject("content is required")
            return
        }
        pluginScope.launch {
            try {
                facade.sendMessage(content)
                call.resolve()
            } catch (e: IllegalArgumentException) {
                call.reject(e.message ?: "invalid content")
            } catch (e: Exception) {
                call.reject("sendMessage failed: ${e.message}", e)
            }
        }
    }

    @PluginMethod
    fun cancelTask(call: PluginCall) {
        try {
            facade.cancelTask()
            call.resolve()
        } catch (e: Exception) {
            call.reject("cancelTask failed: ${e.message}", e)
        }
    }

    @PluginMethod
    fun clearMessages(call: PluginCall) {
        pluginScope.launch {
            try {
                facade.clearMessages()
                call.resolve()
            } catch (e: Exception) {
                call.reject("clearMessages failed: ${e.message}", e)
            }
        }
    }
}
