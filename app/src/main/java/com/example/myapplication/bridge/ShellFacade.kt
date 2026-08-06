package com.example.myapplication.bridge

import android.content.Context
import com.example.myapplication.accessibility.AutoService
import com.example.myapplication.shell.ShellExecutor
import com.example.myapplication.shell.ShizukuHelper
import com.example.myapplication.utils.Logger

/**
 * Debug shell façade for LingxiShell (docs/bridge-api.md §3.6).
 * Also exposes package-resolution helpers used by Debug / TypeTool pages.
 */
class ShellFacade(
    private val appContext: Context,
) {
    private val logger = Logger("ShellFacade")
    private val shellExecutor = ShellExecutor(appContext)

    data class ShizukuStatusDto(
        val ready: Boolean,
        val available: Boolean,
        val status: String,
    )

    data class CommandResultDto(
        val success: Boolean,
        val output: String,
        val error: String? = null,
        val exitCode: Int? = null,
    )

    data class PackageInfoDto(
        val packageName: String,
        val label: String,
        val isSystem: Boolean = false,
        val hasLaunchIntent: Boolean = true,
    )

    data class ShellTestResultDto(
        val name: String,
        val success: Boolean,
        val message: String,
        val durationMs: Long,
    )

    fun getShizukuStatus(): ShizukuStatusDto {
        val ready = ShizukuHelper.isReady()
        val available = ShizukuHelper.isAvailable()
        val status = when {
            ready -> "ready"
            available -> "available"
            else -> "unavailable"
        }
        return ShizukuStatusDto(ready = ready, available = available, status = status)
    }

    suspend fun runCommand(command: String): CommandResultDto {
        if (command.isBlank()) {
            return CommandResultDto(success = false, output = "", error = "command is required")
        }
        return if (ShizukuHelper.isReady()) {
            val result = ShizukuHelper.execute(command)
            CommandResultDto(
                success = result.isSuccess,
                output = result.output,
                error = result.error.takeIf { it.isNotBlank() },
                exitCode = result.exitCode,
            )
        } else {
            // Fallback: Runtime.exec (limited)
            try {
                val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
                val out = process.inputStream.bufferedReader().readText()
                val err = process.errorStream.bufferedReader().readText()
                val code = process.waitFor()
                CommandResultDto(
                    success = code == 0,
                    output = out,
                    error = err.takeIf { it.isNotBlank() },
                    exitCode = code,
                )
            } catch (e: Exception) {
                logger.e("runCommand failed: ${e.message}", e)
                CommandResultDto(success = false, output = "", error = e.message)
            }
        }
    }

    suspend fun listPackages(
        includeSystem: Boolean = false,
        limit: Int = 50,
    ): List<PackageInfoDto> {
        val result = shellExecutor.listAllApps(
            includeSystem = includeSystem,
            includeNonLaunchable = false,
        )
        val apps = result.getOrElse {
            logger.w("listPackages failed: ${it.message}")
            emptyList()
        }
        return apps
            .sortedBy { it.label.lowercase() }
            .take(limit.coerceAtLeast(1).coerceAtMost(500))
            .map {
                PackageInfoDto(
                    packageName = it.packageName,
                    label = it.label,
                    isSystem = it.isSystem,
                    hasLaunchIntent = it.hasLaunchIntent,
                )
            }
    }

    suspend fun launchApp(nameOrPackage: String): CommandResultDto {
        if (nameOrPackage.isBlank()) {
            return CommandResultDto(success = false, output = "", error = "nameOrPackage is required")
        }
        val result = shellExecutor.launchApp(nameOrPackage)
        return if (result.isSuccess) {
            CommandResultDto(success = true, output = result.getOrNull() ?: "ok")
        } else {
            CommandResultDto(
                success = false,
                output = "",
                error = result.exceptionOrNull()?.message ?: "launch failed",
            )
        }
    }

    /**
     * Direct AutoService.inputText for TypeTool test page.
     * Caller must focus a text field first.
     */
    fun inputText(text: String): CommandResultDto {
        val service = AutoService.getInstance()
            ?: return CommandResultDto(
                success = false,
                output = "",
                error = "无障碍服务未启动",
            )
        return try {
            val ok = service.inputText(text)
            if (ok) {
                CommandResultDto(success = true, output = "inputText ok: '$text'")
            } else {
                CommandResultDto(
                    success = false,
                    output = "",
                    error = "执行失败: 请确保输入框已获取焦点",
                )
            }
        } catch (e: Exception) {
            CommandResultDto(success = false, output = "", error = e.message)
        }
    }

    /** Suite mirroring Compose DebugTestScreen package/shell checks. */
    suspend fun runPackageTests(): List<ShellTestResultDto> {
        val results = mutableListOf<ShellTestResultDto>()

        results += timed("常用应用映射") {
            val map = shellExecutor.getCommonAppsMapForTest()
            val tests = mapOf(
                "微信" to "com.tencent.mm",
                "抖音" to "com.ss.android.ugc.aweme",
                "设置" to "com.android.settings",
            )
            val failed = tests.filter { (k, v) -> map[k.lowercase()] != v && map[k] != v }
            if (failed.isEmpty()) {
                "✓ 全部通过 (${tests.size}个)" to true
            } else {
                "✗ 失败: $failed" to false
            }
        }

        results += timed("包名解析") {
            val tests = mapOf(
                "微信" to "com.tencent.mm",
                "com.tencent.mm" to "com.tencent.mm",
            )
            val failed = tests.filter { (input, expected) ->
                shellExecutor.resolvePackageNameForTest(input) != expected
            }
            if (failed.isEmpty()) "✓ 全部通过" to true else "✗ 失败: $failed" to false
        }

        results += timed("PackageManager 列表") {
            val apps = appContext.packageManager.getInstalledApplications(0)
                .filter { appContext.packageManager.getLaunchIntentForPackage(it.packageName) != null }
            if (apps.isNotEmpty()) {
                "✓ 找到 ${apps.size} 个应用" to true
            } else {
                "✗ 未找到应用" to false
            }
        }

        results += timed("启动Intent检查") {
            val packages = listOf(
                "com.tencent.mm" to "微信",
                "com.android.settings" to "设置",
            )
            val lines = packages.map { (pkg, name) ->
                val intent = appContext.packageManager.getLaunchIntentForPackage(pkg)
                "${if (intent != null) "✓" else "✗"} $name"
            }
            val ok = lines.all { it.startsWith("✓") }
            lines.joinToString("\n") to ok
        }

        results += timed("应用名查找") {
            val found = shellExecutor.findAppByName("微信")
            if (found == "com.tencent.mm") {
                "✓ 微信 -> com.tencent.mm" to true
            } else {
                "✗ 结果: $found" to false
            }
        }

        return results
    }

    private suspend fun timed(
        name: String,
        block: suspend () -> Pair<String, Boolean>,
    ): ShellTestResultDto {
        val start = System.currentTimeMillis()
        return try {
            val (message, success) = block()
            ShellTestResultDto(name, success, message, System.currentTimeMillis() - start)
        } catch (e: Exception) {
            ShellTestResultDto(name, false, "异常: ${e.message}", System.currentTimeMillis() - start)
        }
    }
}
