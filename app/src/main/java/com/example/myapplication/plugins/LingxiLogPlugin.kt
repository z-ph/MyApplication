package com.example.myapplication.plugins

import com.example.myapplication.bridge.LogFacade
import com.example.myapplication.di.ServiceLocator
import com.getcapacitor.JSArray
import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * Capacitor plugin: runtime log buffer.
 * Contract: docs/bridge-api.md §3.5 LingxiLog
 */
@CapacitorPlugin(name = "LingxiLog")
class LingxiLogPlugin : Plugin() {

    private val pluginScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var logsJob: Job? = null

    private val facade: LogFacade
        get() = ServiceLocator.getLogFacade()

    override fun load() {
        super.load()
        startEventPipe()
    }

    override fun handleOnDestroy() {
        logsJob?.cancel()
        super.handleOnDestroy()
    }

    private fun startEventPipe() {
        logsJob?.cancel()
        logsJob = pluginScope.launch {
            facade.logsFlow().collectLatest { logs ->
                notifyListeners("logAppended", logsEnvelope(logs))
            }
        }
    }

    @PluginMethod
    fun list(call: PluginCall) {
        try {
            call.resolve(logsEnvelope(facade.list()))
        } catch (e: Exception) {
            call.reject("list failed: ${e.message}", e)
        }
    }

    @PluginMethod
    fun clear(call: PluginCall) {
        try {
            facade.clear()
            call.resolve()
        } catch (e: Exception) {
            call.reject("clear failed: ${e.message}", e)
        }
    }

    @PluginMethod
    fun export(call: PluginCall) {
        try {
            val text = facade.exportAsText()
            val ret = JSObject()
            ret.put("text", text)
            call.resolve(ret)
        } catch (e: Exception) {
            call.reject("export failed: ${e.message}", e)
        }
    }

    private fun logsEnvelope(logs: List<LogFacade.LogEntryDto>): JSObject = JSObject().apply {
        put(
            "logs",
            JSArray().also { arr ->
                logs.forEach { arr.put(logEntry(it)) }
            },
        )
    }

    private fun logEntry(dto: LogFacade.LogEntryDto): JSObject = JSObject().apply {
        put("id", dto.id)
        put("timestamp", dto.timestamp)
        put("tag", dto.tag)
        put("level", dto.level)
        put("message", dto.message)
        if (dto.throwable == null) {
            put("throwable", JSONObject.NULL)
        } else {
            put("throwable", dto.throwable)
        }
    }
}
