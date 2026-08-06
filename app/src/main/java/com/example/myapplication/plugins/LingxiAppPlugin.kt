package com.example.myapplication.plugins

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin

/**
 * Phase 1 empty-bridge plugin: smoke test + version info.
 * Contract: docs/bridge-api.md §3.7 LingxiApp (+ echo probe).
 */
@CapacitorPlugin(name = "LingxiApp")
class LingxiAppPlugin : Plugin() {

    @PluginMethod
    fun echo(call: PluginCall) {
        val value = call.getString("value") ?: ""
        val ret = JSObject()
        ret.put("value", value)
        call.resolve(ret)
    }

    @PluginMethod
    fun getVersion(call: PluginCall) {
        val ctx = context
        val pm = ctx.packageManager
        val packageName = ctx.packageName
        val pInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(packageName, 0)
        }
        val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            pInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            pInfo.versionCode.toLong()
        }
        val appLabel = try {
            val appInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getApplicationInfo(packageName, 0)
            }
            pm.getApplicationLabel(appInfo)?.toString() ?: "灵犀"
        } catch (_: Exception) {
            "灵犀"
        }

        val ret = JSObject()
        ret.put("appName", appLabel)
        ret.put("packageName", packageName)
        ret.put("versionName", pInfo.versionName ?: "")
        ret.put("versionCode", versionCode)
        ret.put("mock", false)
        call.resolve(ret)
    }

    @PluginMethod
    fun openUrl(call: PluginCall) {
        val url = call.getString("url")
        if (url.isNullOrBlank()) {
            call.reject("url is required")
            return
        }
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            call.resolve()
        } catch (e: Exception) {
            call.reject("Failed to open url: ${e.message}", e)
        }
    }
}
