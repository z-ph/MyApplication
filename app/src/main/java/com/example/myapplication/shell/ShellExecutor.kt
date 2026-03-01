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
            // Method 1: Try standard Intent launch with known package names
            val resolvedPackageName = resolvePackageName(packageName)

            val intent = pm.getLaunchIntentForPackage(resolvedPackageName)
            if (intent != null) {
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                val label = getAppLabel(resolvedPackageName)
                logger.i("Launched app via Intent: $resolvedPackageName")
                return@withContext Result.success("已启动应用: $label ($resolvedPackageName)")
            }

            // Method 2: Try using monkey command via Shizuku
            if (ShizukuHelper.isReady()) {
                val result = ShizukuHelper.execute(
                    "monkey -p $resolvedPackageName -c android.intent.category.LAUNCHER 1"
                )
                if (result.isSuccess) {
                    val label = getAppLabel(resolvedPackageName)
                    logger.i("Launched app via monkey: $resolvedPackageName")
                    return@withContext Result.success("已启动应用: $label ($resolvedPackageName)")
                }
            }

            // Method 3: Try using am start via Shizuku
            if (ShizukuHelper.isReady()) {
                // Get the main activity
                val dumpResult = ShizukuHelper.execute("dumpsys package $resolvedPackageName | grep -A 1 'android.intent.action.MAIN'")
                if (dumpResult.hasOutput) {
                    val activityMatch = Regex("([\\w.]+)/([\\w.]+)").find(dumpResult.output)
                    if (activityMatch != null) {
                        val pkg = activityMatch.groupValues[1]
                        val activity = activityMatch.groupValues[2]
                        val startResult = ShizukuHelper.execute("am start -n $pkg/$activity")
                        if (startResult.isSuccess) {
                            val label = getAppLabel(resolvedPackageName)
                            logger.i("Launched app via am start: $resolvedPackageName")
                            return@withContext Result.success("已启动应用: $label ($resolvedPackageName)")
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
     * Resolve package name from app name or partial package name
     * Uses a predefined map for common apps as fallback
     */
    private suspend fun resolvePackageName(input: String): String {
        // If it looks like a package name, use it directly
        if (input.contains(".") && input.count { it == '.' } >= 2) {
            return input
        }

        // Try to find via ShellExecutor's app list
        val found = findAppByName(input)
        if (found != null) {
            return found
        }

        // Return original input as last resort
        return input
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
        // First check common apps mapping
        val commonApps = COMMON_APPS_MAP
        commonApps[appName.lowercase()]?.let { return@withContext it }

        // Then try dynamic search
        val result = listAllApps(includeSystem = false, includeNonLaunchable = false)
        if (result.isFailure) {
            // Fallback to common apps contains match
            for ((name, pkg) in commonApps) {
                if (name.contains(appName, ignoreCase = true) || appName.contains(name, ignoreCase = true)) {
                    return@withContext pkg
                }
            }
            return@withContext null
        }

        val apps = result.getOrNull() ?: return@withContext null

        // Exact match first
        apps.find { it.label.equals(appName, ignoreCase = true) }?.packageName?.let { return@withContext it }

        // Contains match
        apps.find { it.label.contains(appName, ignoreCase = true) }?.packageName?.let { return@withContext it }

        // Package name match
        apps.find { it.packageName.contains(appName, ignoreCase = true) }?.packageName?.let { return@withContext it }

        // Final fallback: common apps contains match
        for ((name, pkg) in commonApps) {
            if (name.contains(appName, ignoreCase = true) || appName.contains(name, ignoreCase = true)) {
                return@withContext pkg
            }
        }

        null
    }

    /**
     * Get common apps mapping for testing
     */
    fun getCommonAppsMapForTest(): Map<String, String> = COMMON_APPS_MAP

    /**
     * Resolve package name for testing
     */
    suspend fun resolvePackageNameForTest(input: String): String {
        return resolvePackageName(input)
    }

    /**
     * Enable an app (if disabled)
     * Requires Shizuku
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
     * Requires Shizuku
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

    companion object {
        val COMMON_APPS_MAP = mapOf(
            "微信" to "com.tencent.mm",
            "weixin" to "com.tencent.mm",
            "qq" to "com.tencent.mobileqq",
            "qq音乐" to "com.tencent.qqmusic",
            "网易云音乐" to "com.netease.cloudmusic",
            "抖音" to "com.ss.android.ugc.aweme",
            "douyin" to "com.ss.android.ugc.aweme",
            "快手" to "com.smile.gifmaker",
            "淘宝" to "com.taobao.taobao",
            "taobao" to "com.taobao.taobao",
            "天猫" to "com.tmall.wireless",
            "京东" to "com.jingdong.app.mall",
            "jd" to "com.jingdong.app.mall",
            "支付宝" to "com.eg.android.AlipayGphone",
            "alipay" to "com.eg.android.AlipayGphone",
            "微博" to "com.sina.weibo",
            "weibo" to "com.sina.weibo",
            "小红书" to "com.xingin.xhs",
            "哔哩哔哩" to "tv.danmaku.bili",
            "bilibili" to "tv.danmaku.bili",
            "b站" to "tv.danmaku.bili",
            "美团" to "com.sankuai.meituan",
            "饿了么" to "me.ele",
            "高德地图" to "com.autonavi.minimap",
            "百度地图" to "com.baidu.BaiduMap",
            "滴滴" to "com.sdu.didi.psnger",
            "设置" to "com.android.settings",
            "settings" to "com.android.settings",
            "相机" to "com.android.camera",
            "camera" to "com.android.camera",
            "相册" to "com.android.gallery3d",
            "gallery" to "com.android.gallery3d",
            "计算器" to "com.android.calculator2",
            "calculator" to "com.android.calculator2",
            "时钟" to "com.android.deskclock",
            "clock" to "com.android.deskclock",
            "日历" to "com.android.calendar",
            "calendar" to "com.android.calendar",
            "文件管理" to "com.android.filemanager",
            "files" to "com.android.filemanager",
            "浏览器" to "com.android.browser",
            "browser" to "com.android.browser",
            "电话" to "com.android.dialer",
            "phone" to "com.android.dialer",
            "短信" to "com.android.mms",
            "messages" to "com.android.mms",
            "联系人" to "com.android.contacts",
            "contacts" to "com.android.contacts"
        )
    }
}
