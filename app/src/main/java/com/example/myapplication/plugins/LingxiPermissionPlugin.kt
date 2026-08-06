package com.example.myapplication.plugins

import android.app.Activity
import androidx.activity.result.ActivityResult
import com.example.myapplication.bridge.PermissionFacade
import com.example.myapplication.di.ServiceLocator
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.ActivityCallback
import com.getcapacitor.annotation.CapacitorPlugin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Capacitor plugin: permission status / settings / MediaProjection.
 * Contract: docs/bridge-api.md §3.4 LingxiPermission
 *
 * requestScreenCapture uses Activity Result via BridgeActivity launcher.
 */
@CapacitorPlugin(name = "LingxiPermission")
class LingxiPermissionPlugin : Plugin() {

    private val pluginScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val facade: PermissionFacade
        get() = ServiceLocator.getPermissionFacade(context.applicationContext)

    @PluginMethod
    fun getStatus(call: PluginCall) {
        pluginScope.launch {
            try {
                val status = facade.getStatus()
                call.resolve(PluginJson.permissionStatus(status))
            } catch (e: Exception) {
                call.reject("getStatus failed: ${e.message}", e)
            }
        }
    }

    @PluginMethod
    fun openAccessibilitySettings(call: PluginCall) {
        try {
            activity.startActivity(facade.openAccessibilitySettings())
            call.resolve()
        } catch (e: Exception) {
            call.reject("openAccessibilitySettings failed: ${e.message}", e)
        }
    }

    @PluginMethod
    fun requestOverlay(call: PluginCall) {
        try {
            activity.startActivity(facade.openOverlaySettings())
            call.resolve()
        } catch (e: Exception) {
            call.reject("requestOverlay failed: ${e.message}", e)
        }
    }

    @PluginMethod
    fun requestScreenCapture(call: PluginCall) {
        try {
            val intent = facade.createScreenCaptureIntent()
            startActivityForResult(call, intent, "screenCaptureResult")
        } catch (e: Exception) {
            call.reject("requestScreenCapture failed: ${e.message}", e)
        }
    }

    @ActivityCallback
    private fun screenCaptureResult(call: PluginCall?, result: ActivityResult) {
        if (call == null) {
            return
        }
        pluginScope.launch {
            try {
                if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                    val ok = facade.applyScreenCaptureResult(result.resultCode, result.data!!)
                    if (!ok) {
                        call.reject("Failed to start screen capture service")
                        emitStatus()
                        return@launch
                    }
                } else {
                    call.reject("Screen capture permission denied")
                    emitStatus()
                    return@launch
                }
                val status = facade.getStatus()
                call.resolve(PluginJson.permissionStatus(status))
                notifyListeners("statusChanged", PluginJson.permissionStatus(status))
            } catch (e: Exception) {
                call.reject("screenCaptureResult failed: ${e.message}", e)
            }
        }
    }

    @PluginMethod
    fun refresh(call: PluginCall) {
        pluginScope.launch {
            try {
                val status = facade.getStatus()
                val payload = PluginJson.permissionStatus(status)
                call.resolve(payload)
                notifyListeners("statusChanged", payload)
            } catch (e: Exception) {
                call.reject("refresh failed: ${e.message}", e)
            }
        }
    }

    private suspend fun emitStatus() {
        try {
            val status = facade.getStatus()
            notifyListeners("statusChanged", PluginJson.permissionStatus(status))
        } catch (_: Exception) {
            // ignore
        }
    }
}
