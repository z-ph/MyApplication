package com.example.myapplication.plugins

import com.example.myapplication.bridge.AgentFacade
import com.example.myapplication.di.ServiceLocator
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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * Capacitor plugin: agent state / reconfigure / isConfigured.
 * Contract: docs/bridge-api.md §3.2 LingxiAgent
 *
 * getState and stateChanged return bare AgentStateDto (no outer wrapper).
 */
@CapacitorPlugin(name = "LingxiAgent")
class LingxiAgentPlugin : Plugin() {

    private val pluginScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var stateJob: Job? = null

    private val facade: AgentFacade
        get() = ServiceLocator.getAgentFacade(context.applicationContext)

    override fun load() {
        super.load()
        // Ensure chat façade (and cancel flag linkage) is ready.
        ServiceLocator.getChatFacade(context.applicationContext).ensureInitialized()
        startStatePipe()
    }

    override fun handleOnDestroy() {
        stateJob?.cancel()
        super.handleOnDestroy()
    }

    private fun startStatePipe() {
        stateJob?.cancel()
        stateJob = pluginScope.launch {
            // distinctUntilChanged reduces event storms (bridge-api §5).
            facade.stateFlow()
                .distinctUntilChanged()
                .collectLatest { dto ->
                    notifyListeners("stateChanged", PluginJson.agentState(dto))
                }
        }
    }

    @PluginMethod
    fun getState(call: PluginCall) {
        try {
            call.resolve(PluginJson.agentState(facade.getState()))
        } catch (e: Exception) {
            call.reject("getState failed: ${e.message}", e)
        }
    }

    @PluginMethod
    fun reconfigure(call: PluginCall) {
        try {
            val result = facade.reconfigure()
            if (result.isSuccess) {
                call.resolve(JSObject().apply { put("ok", true) })
            } else {
                val err = result.exceptionOrNull()
                call.reject(
                    "reconfigure failed: ${err?.message}",
                    err as? Exception ?: Exception(err?.message),
                )
            }
        } catch (e: Exception) {
            call.reject("reconfigure failed: ${e.message}", e)
        }
    }

    @PluginMethod
    fun isConfigured(call: PluginCall) {
        pluginScope.launch {
            try {
                val configured = facade.isConfigured()
                call.resolve(JSObject().apply { put("configured", configured) })
            } catch (e: Exception) {
                call.reject("isConfigured failed: ${e.message}", e)
            }
        }
    }
}
