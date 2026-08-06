package com.example.myapplication

import android.os.Bundle
import com.example.myapplication.plugins.LingxiAgentPlugin
import com.example.myapplication.plugins.LingxiApiConfigPlugin
import com.example.myapplication.plugins.LingxiAppPlugin
import com.example.myapplication.plugins.LingxiChatPlugin
import com.example.myapplication.plugins.LingxiPermissionPlugin
import com.getcapacitor.BridgeActivity

/**
 * Capacitor host activity. Compose UI shell removed in Phase 1;
 * H5 lives in assets/public. Compose screens remain in codebase until Phase 4 cleanup.
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
        super.onCreate(savedInstanceState)
    }
}
