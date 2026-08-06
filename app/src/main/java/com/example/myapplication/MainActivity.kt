package com.example.myapplication

import android.os.Bundle
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
    }
}
