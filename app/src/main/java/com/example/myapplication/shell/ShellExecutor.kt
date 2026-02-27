package com.example.myapplication.shell

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.example.myapplication.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * High-level shell operations using Shizuku
 * Provides reliable app listing and launching capabilities
 */
class ShellExecutor(private val context: Context) {

    private val logger = Logger("ShellExecutor")
    private val pm = context.packageManager

    /**
     * App info data class
     */
    data class AppInfo(
        val packageName: String,
        val label: String,
        val isSystem: Boolean,
        val hasLaunchIntent: Boolean
    )

    /**
     * Check if shell commands are available
     */
    fun isShellAvailable(): Boolean {
        return ShizukuHelper.isReady()
    }

    /**
     * List all installed applications
     * Uses Shizuku to bypass QUERY_ALL_PACKAGES limitations
     *
     * @param includeSystem Include system apps in the result
     * @param includeNonLaunchable Include apps without launch intent
     * @return List of AppInfo
     */
    suspend fun listAllApps(
        includeSystem: Boolean = false,
        includeNonLaunchable: Boolean = false
    ): Result<List<AppInfo>> = withContext(Dispatchers.IO) {
        try {
            // Try Shizuku first for complete list
            if (ShizukuHelper.isReady()) {
                logger.d("Listing apps via Shizuku")
                return@withContext listAppsViaShizuku(includeSystem, includeNonLaunchable)
            }

            // Fallback to PackageManager (may be limited)
            logger.d("Listing apps via PackageManager (fallback)")
            return@withContext listAppsViaPackageManager(includeSystem, includeNonLaunchable)
        } catch (e: Exception) {
            logger.e("Failed to list apps: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * List apps using Shizuku shell commands
     */
    private suspend fun listAppsViaShizuku(
        includeSystem: Boolean,
        includeNonLaunchable: Boolean
    ): Result<List<AppInfo>> {
        val result = if (includeSystem) {
            ShizukuHelper.execute("pm list packages -f")
        } else {
            // -3 flag lists only third-party apps
            ShizukuHelper.execute("pm list packages -3 -f")
        }

        if (!result.isSuccess) {
            logger.w("Shizuku command failed: ${result.error}")
            return listAppsViaPackageManager(includeSystem, includeNonLaunchable)
        }

        val apps = mutableListOf<AppInfo>()
        val packageLines = result.output.split("\n").filter { it.startsWith("package:") }

        for (line in packageLines) {
            // Parse: package:/path/to/apk=package.name
            val packageName = line.substringAfterLast("=").trim()
            if (packageName.isEmpty()) continue

            try {
                val appInfo = pm.getApplicationInfo(packageName, 0)
                val label = pm.getApplicationLabel(appInfo).toString()
                val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val launchIntent = pm.getLaunchIntentForPackage(packageName)
                val hasLaunchIntent = launchIntent != null

                if (includeNonLaunchable || hasLaunchIntent) {
                    apps.add(AppInfo(packageName, label, isSystem, hasLaunchIntent))
                }
            } catch (e: Exception) {
                // Package might not be accessible, skip it
                logger.d("Skipping inaccessible package: $packageName")
            }
        }

        logger.i("Listed ${apps.size} apps via Shizuku")
        return Result.success(apps.sortedBy { it.label.lowercase() })
    }

    /**
     * Fallback: List apps using PackageManager
     */
    private fun listAppsViaPackageManager(
        includeSystem: Boolean,
        includeNonLaunchable: Boolean
    ): Result<List<AppInfo>> {
        val apps = mutableListOf<AppInfo>()

        try {
            val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)

            for (appInfo in installedApps) {
                val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                if (!includeSystem && isSystem) continue

                val launchIntent = pm.getLaunchIntentForPackage(appInfo.packageName)
                val hasLaunchIntent = launchIntent != null

                if (includeNonLaunchable || hasLaunchIntent) {
                    val label = pm.getApplicationLabel(appInfo).toString()
                    apps.add(AppInfo(appInfo.packageName, label, isSystem, hasLaunchIntent))
                }
            }

            logger.i("Listed ${apps.size} apps via PackageManager")
            return Result.success(apps.sortedBy { it.label.lowercase() })
        } catch (e: Exception) {
            logger.e("PackageManager listing failed: ${e.message}")
            return Result.failure(e)
        }
    }

    /**
     * Launch an app by package name
     * Uses multiple methods for maximum reliability
     *
     * @param packageName The package name to launch
     * @return Result with success/failure message
     */
    suspend fun launchApp(packageName: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Method 1: Try standard Intent launch
            val intent = pm.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                val label = getAppLabel(packageName)
                logger.i("Launched app via Intent: $packageName")
                return@withContext Result.success("已启动应用: $label ($packageName)")
            }

            // Method 2: Try using monkey command via Shizuku
            if (ShizukuHelper.isReady()) {
                val result = ShizukuHelper.execute(
                    "monkey -p $packageName -c android.intent.category.LAUNCHER 1"
                )
                if (result.isSuccess) {
                    val label = getAppLabel(packageName)
                    logger.i("Launched app via monkey: $packageName")
                    return@withContext Result.success("已启动应用: $label ($packageName)")
                }
            }

            // Method 3: Try using am start via Shizuku
            if (ShizukuHelper.isReady()) {
                // Get the main activity
                val dumpResult = ShizukuHelper.execute("dumpsys package $packageName | grep -A 1 'android.intent.action.MAIN'")
                if (dumpResult.hasOutput) {
                    val activityMatch = Regex("([\\w.]+)/([\\w.]+)").find(dumpResult.output)
                    if (activityMatch != null) {
                        val pkg = activityMatch.groupValues[1]
                        val activity = activityMatch.groupValues[2]
                        val startResult = ShizukuHelper.execute("am start -n $pkg/$activity")
                        if (startResult.isSuccess) {
                            val label = getAppLabel(packageName)
                            logger.i("Launched app via am start: $packageName")
                            return@withContext Result.success("已启动应用: $label ($packageName)")
                        }
                    }
                }
            }

            Result.failure(Exception("无法启动应用: $packageName。该应用可能没有可启动的界面。"))
        } catch (e: Exception) {
            logger.e("Failed to launch app $packageName: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Force stop an app
     * Requires Shizuku
     */
    suspend fun forceStopApp(packageName: String): Result<String> = withContext(Dispatchers.IO) {
        if (!ShizukuHelper.isReady()) {
            return@withContext Result.failure(Exception("Shizuku 未就绪，无法强制停止应用"))
        }

        val result = ShizukuHelper.execute("am force-stop $packageName")
        if (result.isSuccess) {
            logger.i("Force stopped: $packageName")
            Result.success("已强制停止应用: $packageName")
        } else {
            logger.e("Failed to force stop $packageName: ${result.error}")
            Result.failure(Exception("强制停止失败: ${result.error}"))
        }
    }

    /**
     * Clear app data
     * Requires Shizuku
     */
    suspend fun clearAppData(packageName: String): Result<String> = withContext(Dispatchers.IO) {
        if (!ShizukuHelper.isReady()) {
            return@withContext Result.failure(Exception("Shizuku 未就绪，无法清除应用数据"))
        }

        val result = ShizukuHelper.execute("pm clear $packageName")
        if (result.isSuccess) {
            logger.i("Cleared data for: $packageName")
            Result.success("已清除应用数据: $packageName")
        } else {
            logger.e("Failed to clear data for $packageName: ${result.error}")
            Result.failure(Exception("清除数据失败: ${result.error}"))
        }
    }

    /**
     * Check if an app is installed
     */
    suspend fun isAppInstalled(packageName: String): Boolean = withContext(Dispatchers.IO) {
        if (ShizukuHelper.isReady()) {
            val result = ShizukuHelper.execute("pm path $packageName")
            return@withContext result.isSuccess && result.hasOutput
        }

        try {
            pm.getPackageInfo(packageName, 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get app label by package name
     */
    private fun getAppLabel(packageName: String): String {
        return try {
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName
        }
    }

    /**
     * Find app by name (fuzzy search)
     * Returns the package name if found
     */
    suspend fun findAppByName(appName: String): String? = withContext(Dispatchers.IO) {
        val result = listAllApps(includeSystem = false, includeNonLaunchable = false)
        if (result.isFailure) return@withContext null

        val apps = result.getOrNull() ?: return@withContext null

        // Exact match first
        apps.find { it.label.equals(appName, ignoreCase = true) }?.packageName?.let { return@withContext it }

        // Contains match
        apps.find { it.label.contains(appName, ignoreCase = true) }?.packageName?.let { return@withContext it }

        // Package name match
        apps.find { it.packageName.contains(appName, ignoreCase = true) }?.packageName?.let { return@withContext it }

        null
    }

    /**
     * Enable an app (if disabled)
     */
    suspend fun enableApp(packageName: String): Result<String> = withContext(Dispatchers.IO) {
        if (!ShizukuHelper.isReady()) {
            return@withContext Result.failure(Exception("Shizuku 未就绪"))
        }

        val result = ShizukuHelper.execute("pm enable $packageName")
        if (result.isSuccess) {
            Result.success("已启用应用: $packageName")
        } else {
            Result.failure(Exception("启用失败: ${result.error}"))
        }
    }

    /**
     * Disable an app
     */
    suspend fun disableApp(packageName: String): Result<String> = withContext(Dispatchers.IO) {
        if (!ShizukuHelper.isReady()) {
            return@withContext Result.failure(Exception("Shizuku 未就绪"))
        }

        val result = ShizukuHelper.execute("pm disable $packageName")
        if (result.isSuccess) {
            Result.success("已禁用应用: $packageName")
        } else {
            Result.failure(Exception("禁用失败: ${result.error}"))
        }
    }
}
