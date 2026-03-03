package com.example.myapplication.agent

import android.content.Context
import android.view.accessibility.AccessibilityNodeInfo
import com.example.myapplication.accessibility.AutoService
import com.example.myapplication.api.model.SwipeDirection
import com.example.myapplication.screen.ScreenCapture
import com.example.myapplication.utils.Logger
import dev.langchain4j.agent.tool.P
import dev.langchain4j.agent.tool.Tool
import kotlinx.coroutines.runBlocking

class AndroidTools(
    private val appContext: Context,
    private val screenWidth: Int = DEFAULT_SCREEN_WIDTH,
    private val screenHeight: Int = DEFAULT_SCREEN_HEIGHT,
    private val screenCapture: ScreenCapture = ScreenCapture.getInstance(appContext)
) {

    private val logger = Logger("AndroidTools")
    
    companion object {
        private const val DEFAULT_SCREEN_WIDTH = 1080
        private const val DEFAULT_SCREEN_HEIGHT = 2400
    }

    @Tool("按下返回键")
    fun back(): String {
        val service = AutoService.getInstance() ?: return "❌ 无障碍服务未启用"
        return if (service.pressBack()) "✅ 返回成功" else "❌ 返回失败"
    }

    @Tool("按下 Home 键，回到主屏幕")
    fun home(): String {
        val service = AutoService.getInstance() ?: return "❌ 无障碍服务未启用"
        return if (service.pressHome()) "✅ 回到主页" else "❌ 操作失败"
    }

    @Tool("打开最近任务列表")
    fun recents(): String {
        val service = AutoService.getInstance() ?: return "❌ 无障碍服务未启用"
        return if (service.pressRecents()) "✅ 打开最近任务" else "❌ 操作失败"
    }

    @Tool("点击屏幕指定坐标位置")
    fun click(
        @P("X 坐标 (0-1080)") x: Int,
        @P("Y 坐标 (0-2400)") y: Int
    ): String = runBlocking {
        val service = AutoService.getInstance() ?: return@runBlocking "❌ 无障碍服务未启用"

        val clampedX = x.coerceIn(0, screenWidth)
        val clampedY = y.coerceIn(0, screenHeight)

        val realX = clampedX.toFloat() * screenWidth / 1080f
        val realY = clampedY.toFloat() * screenHeight / 2400f

        logger.d("执行点击：($realX, $realY)")
        service.click(realX, realY)
        "✅ 点击成功 ($clampedX, $clampedY)"
    }

    @Tool("长按屏幕指定坐标")
    fun longClick(
        @P("X 坐标 (0-1080)") x: Int,
        @P("Y 坐标 (0-2400)") y: Int,
        @P("长按时长 (毫秒)，默认 500") duration: Long = 500
    ): String = runBlocking {
        val service = AutoService.getInstance() ?: return@runBlocking "❌ 无障碍服务未启用"
        val realX = x.toFloat() * screenWidth / 1080f
        val realY = y.toFloat() * screenHeight / 2400f
        service.longClick(realX, realY, duration)
        "✅ 长按成功 ($x, $y), 时长=${duration}ms"
    }

    @Tool("在屏幕上滑动")
    fun swipe(
        @P("滑动方向：up/down/left/right") direction: String,
        @P("滑动距离 (像素)，默认 500") distance: Int = 500
    ): String = runBlocking {
        val service = AutoService.getInstance() ?: return@runBlocking "❌ 无障碍服务未启用"

        val swipeDir = when (direction.lowercase()) {
            "up" -> SwipeDirection.UP
            "down" -> SwipeDirection.DOWN
            "left" -> SwipeDirection.LEFT
            "right" -> SwipeDirection.RIGHT
            else -> SwipeDirection.UP
        }

        service.swipe(swipeDir, distance, 300)
        "✅ 滑动成功：$direction, 距离=$distance"
    }

    @Tool("输入文本到当前焦点输入框")
    fun type(
        @P("要输入的文本内容") text: String
    ): String {
        val service = AutoService.getInstance() ?: return "❌ 无障碍服务未启用"

        if (text.isEmpty()) return "❌ 输入文本不能为空"

        return if (service.inputText(text)) {
            val preview = text.take(30) + if (text.length > 30) "..." else ""
            "✅ 输入成功：$preview"
        } else {
            "❌ 输入失败"
        }
    }

    @Tool("根据文本查找屏幕上的控件，返回匹配信息")
    fun findNodesByText(
        @P("要查找的文本内容") text: String,
        @P("是否精确匹配，默认 false（模糊匹配）") exact: Boolean = false
    ): String {
        val service = AutoService.getInstance() ?: return "❌ 无障碍服务未启用"

        val nodes = service.findNodesByText(text, exact)
        if (nodes.isEmpty()) {
            return "❌ 未找到包含\"$text\"的控件"
        }

        val sb = StringBuilder("✅ 找到 ${nodes.size} 个匹配控件:\n")
        nodes.forEachIndexed { index, node ->
            val bounds = android.graphics.Rect()
            node.getBoundsInScreen(bounds)
            val nodeText = node.text?.toString() ?: ""
            sb.appendLine("${index + 1}. \"$nodeText\" at (${bounds.centerX()},${bounds.centerY()})")
        }

        return sb.toString()
    }

    @Tool("根据文本点击屏幕上的控件，优先使用此工具")
    fun clickByText(
        @P("要点击的控件文本") text: String,
        @P("是否精确匹配，默认 false") exact: Boolean = false
    ): String {
        val service = AutoService.getInstance() ?: return "❌ 无障碍服务未启用"

        val nodes = service.findNodesByText(text, exact)
        if (nodes.isEmpty()) {
            return "❌ 未找到包含\"$text\"的控件，尝试使用 find_nodes_by_text 查看可用控件"
        }

        for (node in nodes) {
            if (service.clickNode(node)) {
                val nodeText = node.text?.toString() ?: text
                return "✅ 成功点击 \"$nodeText\""
            }
        }

        return "❌ 找到控件但点击失败，可能需要改用坐标点击"
    }

    @Tool("滚动屏幕直到找到指定文本")
    fun scrollToText(
        @P("要查找的文本") text: String,
        @P("最大滑动次数，默认 10") maxSwipes: Int = 10,
        @P("滑动方向：up/down/auto，默认 auto") direction: String = "auto"
    ): String = runBlocking {
        val service = AutoService.getInstance() ?: return@runBlocking "❌ 无障碍服务未启用"

        var nodes = service.findNodesByText(text, false)
        if (nodes.isNotEmpty()) {
            return@runBlocking "✅ 文本 \"$text\" 已在屏幕上可见"
        }

        val swipeDir = when (direction.lowercase()) {
            "down" -> SwipeDirection.DOWN
            else -> SwipeDirection.UP
        }

        for (i in 1..maxSwipes) {
            service.swipe(swipeDir, 800, 300)
            kotlinx.coroutines.delay(500)

            nodes = service.findNodesByText(text, false)
            if (nodes.isNotEmpty()) {
                return@runBlocking "✅ 滚动 $i 次后找到 \"$text\""
            }
        }

        return@runBlocking "❌ 滚动 $maxSwipes 次后仍未找到 \"$text\""
    }

    @Tool("获取当前屏幕的 UI 控件树信息")
    fun getUiHierarchy(): String {
        val service = AutoService.getInstance() ?: return "❌ 无障碍服务未启用"

        try {
            val root = service.getAccessibilityNodeInfo()
                ?: return "❌ 无法获取控件树根节点"

            val sb = StringBuilder("=== UI 控件树 ===\n\n")
            traverseNode(root, 0, sb)
            return sb.toString()
        } catch (e: Exception) {
            return "❌ 获取控件树失败：${e.message}"
        }
    }

    private fun traverseNode(
        node: AccessibilityNodeInfo,
        depth: Int,
        sb: StringBuilder
    ) {
        if (depth > 10) return

        val indent = "  ".repeat(depth)
        val text = node.text?.toString() ?: ""
        val desc = node.contentDescription?.toString() ?: ""
        val className = node.className?.toString()?.split(".")?.last() ?: "View"
        val bounds = android.graphics.Rect()
        node.getBoundsInScreen(bounds)

        val attrs = mutableListOf<String>()
        if (text.isNotEmpty()) attrs.add("text=\"$text\"")
        if (desc.isNotEmpty()) attrs.add("desc=\"$desc\"")
        attrs.add("pos=(${bounds.centerX()},${bounds.centerY()})")
        if (node.isClickable) attrs.add("clickable")
        if (node.isLongClickable) attrs.add("longClickable")
        if (node.isEditable) attrs.add("editable")
        if (node.isScrollable) attrs.add("scrollable")

        sb.appendLine("$indent$className [${attrs.joinToString(", ")}]")

        for (i in 0 until node.childCount) {
            node.getChild(i)?.let {
                traverseNode(it, depth + 1, sb)
            }
        }
    }

    @Tool("捕获当前屏幕截图")
    fun captureScreen(): String = runBlocking {
        try {
            val bitmap = screenCapture.capture()
                ?: return@runBlocking "❌ 屏幕捕获失败"

            "✅ 屏幕截图成功 (尺寸：${bitmap.width}x${bitmap.height})"
        } catch (e: Exception) {
            "❌ 屏幕截图失败：${e.message}"
        }
    }

    @Tool("打开指定的应用程序")
    fun openApp(
        @P("应用包名或应用名称") packageName: String
    ): String {
        try {
            val pm = appContext.packageManager
            var resolvedPackageName = packageName

            if (!packageName.contains(".")) {
                val apps = pm.getInstalledApplications(0)
                val matchedApp = apps.find {
                    pm.getApplicationLabel(it).toString()
                        .contains(packageName, ignoreCase = true)
                }
                if (matchedApp != null) {
                    resolvedPackageName = matchedApp.packageName
                }
            }

            val intent = pm.getLaunchIntentForPackage(resolvedPackageName)
            if (intent != null) {
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                appContext.startActivity(intent)
                val label = pm.getApplicationLabel(
                    pm.getApplicationInfo(resolvedPackageName, 0)
                )
                return "✅ 打开应用成功：$label ($resolvedPackageName)"
            } else {
                return "❌ 找不到应用：$packageName"
            }
        } catch (e: Exception) {
            return "❌ 打开应用失败：${e.message}"
        }
    }

    @Tool("复制文本到剪贴板")
    fun copyToClipboard(
        @P("要复制的文本") text: String
    ): String {
        val service = AutoService.getInstance() ?: return "❌ 无障碍服务未启用"
        service.copyToClipboard(text)
        return "✅ 已复制到剪贴板：${text.take(30)}"
    }

    @Tool("等待指定时间，用于等待页面加载")
    fun wait(
        @P("等待时间 (毫秒)") ms: Long
    ): String = runBlocking {
        kotlinx.coroutines.delay(ms)
        return@runBlocking "✅ 等待 ${ms}ms"
    }

    @Tool("任务完成，结束执行并报告结果")
    fun finish(
        @P("任务完成总结") summary: String
    ): String {
        return "FINISH:$summary"
    }

    @Tool("回复用户消息")
    fun reply(
        @P("要发送给用户的消息") message: String
    ): String {
        return "REPLY:$message"
    }
}
