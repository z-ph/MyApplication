package com.example.myapplication

import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.webkit.WebView
import com.example.myapplication.plugins.LingxiAgentPlugin
import com.example.myapplication.plugins.LingxiApiConfigPlugin
import com.example.myapplication.plugins.LingxiAppPlugin
import com.example.myapplication.plugins.LingxiChatPlugin
import com.example.myapplication.plugins.LingxiLogPlugin
import com.example.myapplication.plugins.LingxiPermissionPlugin
import com.example.myapplication.plugins.LingxiShellPlugin
import com.getcapacitor.BridgeActivity

/**
 * Capacitor host activity. H5 SPA lives in assets/public (React + antd-mobile).
 * FloatingWindowService / AutoService / ScreenCaptureService stay registered in Manifest.
 *
 * MediaProjection for LingxiPermission.requestScreenCapture is handled via
 * Plugin.startActivityForResult + @ActivityCallback on the BridgeActivity host.
 */
class MainActivity : BridgeActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Must register before super.onCreate so Bridge.Builder picks them up.
        registerPlugin(LingxiAppPlugin::class.java)
        registerPlugin(LingxiChatPlugin::class.java)
        registerPlugin(LingxiAgentPlugin::class.java)
        registerPlugin(LingxiPermissionPlugin::class.java)
        registerPlugin(LingxiApiConfigPlugin::class.java)
        registerPlugin(LingxiLogPlugin::class.java)
        registerPlugin(LingxiShellPlugin::class.java)
        super.onCreate(savedInstanceState)
        hardenWebView()
    }

    /**
     * Post-Bridge init hardening. Capacitor already serves via https://localhost
     * (androidScheme); disable file://-style access and production remote debugging.
     * See docs/bridge-api.md §5 and docs/h5-dev-setup.md.
     */
    private fun hardenWebView() {
        val webView = bridge?.webView ?: return
        val settings = webView.settings
        // App assets are not loaded via file://; keep content:// if needed by plugins.
        settings.allowFileAccess = false
        @Suppress("DEPRECATION")
        settings.allowFileAccessFromFileURLs = false
        @Suppress("DEPRECATION")
        settings.allowUniversalAccessFromFileURLs = false
        // Capacitor defaults debugging to debuggable builds; force off for release.
        val debuggable =
            (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (!debuggable) {
            WebView.setWebContentsDebuggingEnabled(false)
        }
    }
}
