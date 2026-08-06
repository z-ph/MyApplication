package com.example.myapplication.plugins

import com.example.myapplication.bridge.ShellFacade
import com.example.myapplication.di.ServiceLocator
import com.getcapacitor.JSArray
import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * Capacitor plugin: shell / package debug helpers.
 * Contract: docs/bridge-api.md §3.6 LingxiShell
 */
@CapacitorPlugin(name = "LingxiShell")
class LingxiShellPlugin : Plugin() {

    private val pluginScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val facade: ShellFacade
        get() = ServiceLocator.getShellFacade(context.applicationContext)

    @PluginMethod
    fun getShizukuStatus(call: PluginCall) {
        try {
            val s = facade.getShizukuStatus()
            call.resolve(
                JSObject().apply {
                    put("ready", s.ready)
                    put("available", s.available)
                    put("status", s.status)
                },
            )
        } catch (e: Exception) {
            call.reject("getShizukuStatus failed: ${e.message}", e)
        }
    }

    @PluginMethod
    fun runCommand(call: PluginCall) {
        val command = call.getString("command")
        if (command.isNullOrBlank()) {
            call.reject("command is required")
            return
        }
        pluginScope.launch {
            try {
                call.resolve(commandResult(facade.runCommand(command)))
            } catch (e: Exception) {
                call.reject("runCommand failed: ${e.message}", e)
            }
        }
    }

    @PluginMethod
    fun listPackages(call: PluginCall) {
        val includeSystem = call.getBoolean("includeSystem", false) ?: false
        val limit = call.getInt("limit", 50) ?: 50
        pluginScope.launch {
            try {
                val packages = facade.listPackages(includeSystem = includeSystem, limit = limit)
                call.resolve(
                    JSObject().apply {
                        put(
                            "packages",
                            JSArray().also { arr ->
                                packages.forEach { p ->
                                    arr.put(
                                        JSObject().apply {
                                            put("packageName", p.packageName)
                                            put("label", p.label)
                                            put("isSystem", p.isSystem)
                                            put("hasLaunchIntent", p.hasLaunchIntent)
                                        },
                                    )
                                }
                            },
                        )
                    },
                )
            } catch (e: Exception) {
                call.reject("listPackages failed: ${e.message}", e)
            }
        }
    }

    @PluginMethod
    fun launchApp(call: PluginCall) {
        val nameOrPackage = call.getString("nameOrPackage") ?: call.getString("name")
        if (nameOrPackage.isNullOrBlank()) {
            call.reject("nameOrPackage is required")
            return
        }
        pluginScope.launch {
            try {
                call.resolve(commandResult(facade.launchApp(nameOrPackage)))
            } catch (e: Exception) {
                call.reject("launchApp failed: ${e.message}", e)
            }
        }
    }

    @PluginMethod
    fun inputText(call: PluginCall) {
        val text = call.getString("text")
        if (text == null) {
            call.reject("text is required")
            return
        }
        try {
            call.resolve(commandResult(facade.inputText(text)))
        } catch (e: Exception) {
            call.reject("inputText failed: ${e.message}", e)
        }
    }

    @PluginMethod
    fun runPackageTests(call: PluginCall) {
        pluginScope.launch {
            try {
                val results = facade.runPackageTests()
                call.resolve(
                    JSObject().apply {
                        put(
                            "results",
                            JSArray().also { arr ->
                                results.forEach { r ->
                                    arr.put(
                                        JSObject().apply {
                                            put("name", r.name)
                                            put("success", r.success)
                                            put("message", r.message)
                                            put("durationMs", r.durationMs)
                                        },
                                    )
                                }
                            },
                        )
                    },
                )
            } catch (e: Exception) {
                call.reject("runPackageTests failed: ${e.message}", e)
            }
        }
    }

    private fun commandResult(dto: ShellFacade.CommandResultDto): JSObject = JSObject().apply {
        put("success", dto.success)
        put("output", dto.output)
        if (dto.error == null) {
            put("error", JSONObject.NULL)
        } else {
            put("error", dto.error)
        }
        if (dto.exitCode == null) {
            put("exitCode", JSONObject.NULL)
        } else {
            put("exitCode", dto.exitCode)
        }
    }
}
