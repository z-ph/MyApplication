package com.example.myapplication.plugins

import com.example.myapplication.bridge.ApiConfigFacade
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
import kotlinx.coroutines.launch

/**
 * Capacitor plugin: multi-provider API config CRUD + test/fetch models.
 * Contract: docs/bridge-api.md §3.3 LingxiApiConfig
 *
 * List / events return apiKeyMasked only; create/update accept full apiKey.
 */
@CapacitorPlugin(name = "LingxiApiConfig")
class LingxiApiConfigPlugin : Plugin() {

    private val pluginScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var configsJob: Job? = null

    private val facade: ApiConfigFacade
        get() = ServiceLocator.getApiConfigFacade(context.applicationContext)

    override fun load() {
        super.load()
        startConfigsPipe()
    }

    override fun handleOnDestroy() {
        configsJob?.cancel()
        super.handleOnDestroy()
    }

    private fun startConfigsPipe() {
        configsJob?.cancel()
        configsJob = pluginScope.launch {
            facade.configsFlow.collectLatest { configs ->
                notifyListeners("configsChanged", PluginJson.configsEnvelope(configs))
            }
        }
    }

    @PluginMethod
    fun list(call: PluginCall) {
        pluginScope.launch {
            try {
                call.resolve(PluginJson.configsEnvelope(facade.list()))
            } catch (e: Exception) {
                call.reject("list failed: ${e.message}", e)
            }
        }
    }

    @PluginMethod
    fun create(call: PluginCall) {
        val name = call.getString("name") ?: ""
        val provider = call.getString("provider")
        val apiKey = call.getString("apiKey")
        val baseUrl = call.getString("baseUrl") ?: ""
        val modelId = call.getString("modelId") ?: ""
        if (provider.isNullOrBlank()) {
            call.reject("provider is required")
            return
        }
        if (apiKey.isNullOrBlank()) {
            call.reject("apiKey is required")
            return
        }
        pluginScope.launch {
            try {
                val (dto, reconfigured) = facade.create(name, provider, apiKey, baseUrl, modelId)
                call.resolve(PluginJson.configEnvelope(dto, reconfigured))
            } catch (e: IllegalArgumentException) {
                call.reject(e.message ?: "invalid create request")
            } catch (e: Exception) {
                call.reject("create failed: ${e.message}", e)
            }
        }
    }

    @PluginMethod
    fun update(call: PluginCall) {
        val id = call.getString("id")
        if (id.isNullOrBlank()) {
            call.reject("id is required")
            return
        }
        val name = call.getString("name") ?: ""
        val provider = call.getString("provider")
        // apiKey optional on update — blank keeps existing
        val apiKey = call.getString("apiKey")
        val baseUrl = call.getString("baseUrl") ?: ""
        val modelId = call.getString("modelId") ?: ""
        if (provider.isNullOrBlank()) {
            call.reject("provider is required")
            return
        }
        pluginScope.launch {
            try {
                val (dto, reconfigured) = facade.update(id, name, provider, apiKey, baseUrl, modelId)
                call.resolve(PluginJson.configEnvelope(dto, reconfigured))
            } catch (e: IllegalArgumentException) {
                call.reject(e.message ?: "invalid update request")
            } catch (e: Exception) {
                call.reject("update failed: ${e.message}", e)
            }
        }
    }

    @PluginMethod
    fun delete(call: PluginCall) {
        val id = call.getString("id")
        if (id.isNullOrBlank()) {
            call.reject("id is required")
            return
        }
        pluginScope.launch {
            try {
                val reconfigured = facade.delete(id)
                if (reconfigured != null) {
                    call.resolve(PluginJson.reconfiguredEnvelope(reconfigured))
                } else {
                    call.resolve()
                }
            } catch (e: Exception) {
                call.reject("delete failed: ${e.message}", e)
            }
        }
    }

    @PluginMethod
    fun setActive(call: PluginCall) {
        val id = call.getString("id")
        if (id.isNullOrBlank()) {
            call.reject("id is required")
            return
        }
        pluginScope.launch {
            try {
                val reconfigured = facade.setActive(id)
                call.resolve(PluginJson.reconfiguredEnvelope(reconfigured))
            } catch (e: Exception) {
                call.reject("setActive failed: ${e.message}", e)
            }
        }
    }

    @PluginMethod
    fun fetchModels(call: PluginCall) {
        val provider = call.getString("provider")
        val apiKey = call.getString("apiKey") ?: ""
        val baseUrl = call.getString("baseUrl") ?: ""
        // Edit form may leave apiKey blank; configId loads stored secret from Room.
        val configId = call.getString("configId")
        if (provider.isNullOrBlank()) {
            call.reject("provider is required")
            return
        }
        pluginScope.launch {
            try {
                val models = facade.fetchModels(provider, apiKey, baseUrl, configId)
                call.resolve(PluginJson.modelsEnvelope(models))
            } catch (e: Exception) {
                call.reject("fetchModels failed: ${e.message}", e)
            }
        }
    }

    @PluginMethod
    fun testConnection(call: PluginCall) {
        val provider = call.getString("provider")
        val apiKey = call.getString("apiKey") ?: ""
        val baseUrl = call.getString("baseUrl") ?: ""
        val modelId = call.getString("modelId") ?: ""
        // Edit form may leave apiKey blank; configId loads stored secret from Room.
        val configId = call.getString("configId")
        if (provider.isNullOrBlank()) {
            call.reject("provider is required")
            return
        }
        pluginScope.launch {
            try {
                val result = facade.testConnection(provider, apiKey, baseUrl, modelId, configId)
                call.resolve(PluginJson.testConnection(result))
            } catch (e: Exception) {
                call.reject("testConnection failed: ${e.message}", e)
            }
        }
    }

    /** Optional helper for H5 provider Selector defaults. */
    @PluginMethod
    fun listProviders(call: PluginCall) {
        try {
            call.resolve(PluginJson.providersEnvelope(facade.listProviders()))
        } catch (e: Exception) {
            call.reject("listProviders failed: ${e.message}", e)
        }
    }
}
