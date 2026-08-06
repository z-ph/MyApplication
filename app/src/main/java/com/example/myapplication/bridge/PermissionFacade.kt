package com.example.myapplication.bridge

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.example.myapplication.accessibility.AutoService
import com.example.myapplication.data.dto.PermissionStatusDto
import com.example.myapplication.data.mapper.BridgeDtoMapper.permissionStatus
import com.example.myapplication.data.repository.ApiConfigRepository
import com.example.myapplication.screen.ScreenCapture
import com.example.myapplication.shell.ShizukuHelper

/**
 * Permission status + settings intents for LingxiPermission (docs/bridge-api.md §3.4).
 * MediaProjection result is applied by the plugin via [applyScreenCaptureResult].
 */
class PermissionFacade(
    private val appContext: Context,
    private val apiConfigRepository: ApiConfigRepository,
) {

    fun createScreenCaptureIntent(): Intent {
        return ScreenCapture.getInstance(appContext).createCaptureIntent()
    }

    /**
     * After Activity Result OK, start ScreenCaptureService.
     * @return true if projection became active
     */
    suspend fun applyScreenCaptureResult(resultCode: Int, data: Intent): Boolean {
        val screenCapture = ScreenCapture.getInstance(appContext)
        return screenCapture.startCapture(resultCode, data)
    }

    suspend fun getStatus(): PermissionStatusDto {
        val apiConfigured = try {
            apiConfigRepository.getActiveConfig() != null
        } catch (_: Exception) {
            false
        }
        return permissionStatus(
            accessibility = AutoService.isEnabled(),
            overlay = Settings.canDrawOverlays(appContext),
            screenCapture = ScreenCapture.isProjectionActive(),
            appList = checkAppListPermission(),
            notification = checkNotificationPermission(),
            apiConfigured = apiConfigured,
            shizuku = try {
                ShizukuHelper.isReady()
            } catch (_: Exception) {
                false
            },
        )
    }

    fun openAccessibilitySettings(): Intent {
        return Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun openOverlaySettings(): Intent {
        return Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${appContext.packageName}"),
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    private fun checkNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                appContext,
                android.Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun checkAppListPermission(): Boolean {
        return try {
            appContext.packageManager
                .queryIntentActivities(Intent(Intent.ACTION_MAIN), 0)
                .isNotEmpty()
        } catch (_: Exception) {
            false
        }
    }
}
