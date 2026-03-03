package com.example.myapplication.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Path
import android.graphics.Rect
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.myapplication.api.model.SwipeDirection
import com.example.myapplication.utils.Logger
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Accessibility Service for UI Automation
 * Provides core capabilities for automating UI interactions
 */
class AutoService : AccessibilityService() {

    companion object {
        private const val TAG = "AutoService"

        @Volatile
        private var instance: AutoService? = null

        fun getInstance(): AutoService? = instance

        fun isEnabled(): Boolean = instance != null
    }

    private val logger = Logger(TAG)

    // Root node info cache
    private var rootNode: AccessibilityNodeInfo? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        logger.d("AutoService created")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        logger.d("AutoService connected")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        logger.d("AutoService destroyed")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        try {
            event?.let {
                when (it.eventType) {
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
                    AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                        // Update root node cache
                        rootNode = rootInActiveWindow
                    }
                    else -> {
                        // Handle other events if needed
                    }
                }
            }
        } catch (e: Exception) {
            logger.e("Accessibility event error: ${e.message}", e)
            // Clear cached node to force refresh on next access
            rootNode = null
        }
    }

    override fun onInterrupt() {
        logger.d("AutoService interrupted")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        logger.d("AutoService start command")
        return START_STICKY
    }

    /**
     * Get the root accessibility node info
     */
    fun getAccessibilityNodeInfo(): AccessibilityNodeInfo? {
        return rootInActiveWindow ?: rootNode
    }

    /**
     * Perform click at specified coordinates
     *
     * @param x X coordinate in screen pixels
     * @param y Y coordinate in screen pixels
     * @return true if gesture was dispatched successfully
     */
    suspend fun click(x: Float, y: Float): Boolean {
        logger.d("Clicking at ($x, $y)")
        return dispatchGesture(createClickPath(x, y))
    }

    /**
     * Perform click on a specific node
     *
     * @param node The accessibility node to click
     * @return true if click was performed successfully
     */
    fun clickNode(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) {
            logger.w("Cannot click null node")
            return false
        }

        return try {
            // First try to perform click directly on node
            if (node.isClickable) {
                val result = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                if (result) {
                    logger.d("Successfully clicked node directly")
                    return true
                }
            }

            // Try to find parent clickable node
            var parent = node.parent
            while (parent != null) {
                if (parent.isClickable) {
                    val result = parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    if (result) {
                        logger.d("Successfully clicked parent node")
                        return true
                    }
                }
                parent = parent.parent
            }

            // Fall back to coordinate-based click
            val bounds = Rect()
            node.getBoundsInScreen(bounds)
            val centerX = bounds.centerX().toFloat()
            val centerY = bounds.centerY().toFloat()

            logger.d("Clicking node at bounds center ($centerX, $centerY)")
            // Note: This is a suspend function, but we're in a non-suspend context
            // For synchronous calls, use clickNodeAsync
            false
        } catch (e: Exception) {
            logger.e("Error clicking node: ${e.message}")
            false
        }
    }

    /**
     * Asynchronously click on a specific node
     */
    suspend fun clickNodeAsync(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) {
            logger.w("Cannot click null node")
            return false
        }

        // First try direct click
        if (node.isClickable && node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
            logger.d("Successfully clicked node directly")
            return true
        }

        // Try parent nodes
        var parent = node.parent
        while (parent != null) {
            if (parent.isClickable && parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                logger.d("Successfully clicked parent node")
                return true
            }
            parent = parent.parent
        }

        // Fall back to coordinate-based click
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        val centerX = bounds.centerX().toFloat()
        val centerY = bounds.centerY().toFloat()

        return click(centerX, centerY)
    }

    /**
     * Perform swipe gesture
     *
     * @param direction Direction to swipe
     * @param distance Distance to swipe in pixels
     * @param duration Duration of gesture in milliseconds
     * @return true if gesture was dispatched successfully
     */
    suspend fun swipe(direction: SwipeDirection, distance: Int = 500, duration: Long = 300): Boolean {
        val bounds = Rect()
        getAccessibilityNodeInfo()?.getBoundsInScreen(bounds)

        val screenWidth = if (bounds.width() > 0) bounds.width() else 1080
        val screenHeight = if (bounds.height() > 0) bounds.height() else 1920

        val startX: Float
        val startY: Float
        val endX: Float
        val endY: Float

        when (direction) {
            SwipeDirection.UP -> {
                startX = screenWidth / 2f
                startY = screenHeight * 0.7f
                endX = screenWidth / 2f
                endY = screenHeight * 0.3f
            }
            SwipeDirection.DOWN -> {
                startX = screenWidth / 2f
                startY = screenHeight * 0.3f
                endX = screenWidth / 2f
                endY = screenHeight * 0.7f
            }
            SwipeDirection.LEFT -> {
                startX = screenWidth * 0.7f
                startY = screenHeight / 2f
                endX = screenWidth * 0.3f
                endY = screenHeight / 2f
            }
            SwipeDirection.RIGHT -> {
                startX = screenWidth * 0.3f
                startY = screenHeight / 2f
                endX = screenWidth * 0.7f
                endY = screenHeight / 2f
            }
        }

        logger.d("Swiping $direction from ($startX, $startY) to ($endX, $endY)")
        return dispatchGesture(createSwipePath(startX, startY, endX, endY, duration))
    }

    /**
     * Input text
     *
     * @param text Text to input
     * @return true if text was input successfully
     */
    fun inputText(text: String): Boolean {
        logger.d("Inputting text: $text")

        // Find focused input field
        val rootNode = getAccessibilityNodeInfo()
        if (rootNode == null) {
            logger.w("No root node found for text input")
            return false
        }

        // Try to find focused edit text
        val focusedNode = findFocusedEditableNode(rootNode)
        if (focusedNode != null) {
            val result = focusedNode.performAction(
                AccessibilityNodeInfo.ACTION_SET_TEXT,
                Bundle().apply { putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text) }
            )
            if (result) {
                logger.d("Text set successfully on focused node")
                return true
            }
        }

        // Fallback: try to focus and paste
        val editableNode = findFirstEditableNode(rootNode)
        if (editableNode != null) {
            editableNode.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
            return editableNode.performAction(
                AccessibilityNodeInfo.ACTION_SET_TEXT,
                Bundle().apply { putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text) }
            )
        }

        logger.w("No editable node found")
        return false
    }

    /**
     * Press back button
     *
     * @return true if back was performed successfully
     */
    fun pressBack(): Boolean {
        logger.d("Pressing back")
        return performGlobalAction(GLOBAL_ACTION_BACK)
    }

    /**
     * Press home button
     *
     * @return true if home was performed successfully
     */
    fun pressHome(): Boolean {
        logger.d("Pressing home")
        return performGlobalAction(GLOBAL_ACTION_HOME)
    }

    /**
     * Press recents button
     *
     * @return true if recents was performed successfully
     */
    fun pressRecents(): Boolean {
        logger.d("Pressing recents")
        return performGlobalAction(GLOBAL_ACTION_RECENTS)
    }

    /**
     * Dispatch a gesture
     */
    private suspend fun dispatchGesture(path: Path, duration: Long = 100): Boolean = suspendCancellableCoroutine { continuation ->
        val gestureDescription = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, duration))
            .build()

        val result = dispatchGesture(gestureDescription, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                logger.d("Gesture completed successfully")
                continuation.resume(true)
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                logger.w("Gesture cancelled")
                continuation.resume(false)
            }
        }, null)

        if (!result) {
            logger.w("Failed to dispatch gesture")
            continuation.resume(false)
        }
    }

    /**
     * Create a click gesture path
     */
    private fun createClickPath(x: Float, y: Float): Path {
        return Path().apply {
            moveTo(x, y)
        }
    }

    /**
     * Create a swipe gesture path
     */
    private fun createSwipePath(startX: Float, startY: Float, endX: Float, endY: Float, duration: Long): Path {
        return Path().apply {
            moveTo(startX, startY)
            lineTo(endX, endY)
        }
    }

    /**
     * Find the focused editable node
     */
    private fun findFocusedEditableNode(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (root.isFocused && root.isEditable) {
            return root
        }

        for (i in 0 until root.childCount) {
            val child = root.getChild(i) ?: continue
            val result = findFocusedEditableNode(child)
            if (result != null) {
                return result
            }
        }

        return null
    }

    /**
     * Find the first editable node in the tree
     */
    private fun findFirstEditableNode(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (root.isEditable) {
            return root
        }

        for (i in 0 until root.childCount) {
            val child = root.getChild(i) ?: continue
            val result = findFirstEditableNode(child)
            if (result != null) {
                return result
            }
        }

        return null
    }

    // ==================== 更多手势操作 ====================

    /**
     * 长按指定坐标
     */
    suspend fun longClick(x: Float, y: Float, duration: Long = 500): Boolean {
        logger.d("Long clicking at ($x, $y) for ${duration}ms")
        return dispatchGesture(createLongClickPath(x, y, duration), duration)
    }

    /**
     * 长按指定节点
     */
    suspend fun longClickNode(node: AccessibilityNodeInfo?, duration: Long = 500): Boolean {
        if (node == null) return false

        // 先尝试直接长按
        if (node.isLongClickable) {
            val result = node.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK)
            if (result) {
                logger.d("Long clicked node directly")
                return true
            }
        }

        // 尝试父节点
        var parent = node.parent
        while (parent != null) {
            if (parent.isLongClickable && parent.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK)) {
                logger.d("Long clicked parent node")
                return true
            }
            parent = parent.parent
        }

        // 回退到坐标长按
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        return longClick(bounds.centerX().toFloat(), bounds.centerY().toFloat(), duration)
    }

    /**
     * 双击坐标
     */
    suspend fun doubleClick(x: Float, y: Float): Boolean {
        logger.d("Double clicking at ($x, $y)")
        // 双击 = 两次快速点击
        val success1 = dispatchGesture(createClickPath(x, y), 50)
        if (!success1) return false
        Thread.sleep(100)
        return dispatchGesture(createClickPath(x, y), 50)
    }

    /**
     * 拖拽 (从一个点拖到另一个点)
     */
    suspend fun drag(startX: Float, startY: Float, endX: Float, endY: Float, duration: Long = 500): Boolean {
        logger.d("Dragging from ($startX, $startY) to ($endX, $endY)")
        return dispatchGesture(createSwipePath(startX, startY, endX, endY, duration), duration)
    }

    /**
     * 双指缩放
     */
    suspend fun pinchZoom(centerX: Float, centerY: Float, scale: Float): Boolean {
        logger.d("Pinch zoom at ($centerX, $centerY) with scale $scale")
        val gestureBuilder = GestureDescription.Builder()

        // 两指从中心向外或向内移动
        val offset = 100f * scale
        val path1 = Path().apply {
            moveTo(centerX - offset, centerY - offset)
            lineTo(centerX - offset * 2, centerY - offset * 2)
        }
        val path2 = Path().apply {
            moveTo(centerX + offset, centerY + offset)
            lineTo(centerX + offset * 2, centerY + offset * 2)
        }

        gestureBuilder.addStroke(GestureDescription.StrokeDescription(path1, 0, 300))
        gestureBuilder.addStroke(GestureDescription.StrokeDescription(path2, 0, 300))

        return dispatchMultiFingerGesture(gestureBuilder.build())
    }

    // ==================== 更多全局操作 ====================

    /**
     * 打开通知栏
     */
    fun openNotifications(): Boolean {
        logger.d("Opening notifications")
        return performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
    }

    /**
     * 打开快速设置
     */
    fun openQuickSettings(): Boolean {
        logger.d("Opening quick settings")
        return performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
    }

    /**
     * 打开电源对话框
     */
    fun openPowerDialog(): Boolean {
        logger.d("Opening power dialog")
        return performGlobalAction(GLOBAL_ACTION_POWER_DIALOG)
    }

    /**
     * 锁屏
     */
    fun lockScreen(): Boolean {
        logger.d("Locking screen")
        return performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
    }

    /**
     * 截屏 (需要Android 9+)
     */
    fun takeScreenshot(): Boolean {
        logger.d("Taking screenshot")
        return performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
    }

    // ==================== 节点操作 ====================

    /**
     * 滚动到指定方向
     */
    fun scrollForward(node: AccessibilityNodeInfo? = null): Boolean {
        val target = node ?: getAccessibilityNodeInfo() ?: return false
        val result = target.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
        logger.d("Scroll forward: $result")
        return result
    }

    fun scrollBackward(node: AccessibilityNodeInfo? = null): Boolean {
        val target = node ?: getAccessibilityNodeInfo() ?: return false
        val result = target.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)
        logger.d("Scroll backward: $result")
        return result
    }

    /**
     * 选择节点
     */
    fun selectNode(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false
        val result = node.performAction(AccessibilityNodeInfo.ACTION_SELECT)
        logger.d("Select node: $result")
        return result
    }

    /**
     * 清除选择
     */
    fun clearSelection(node: AccessibilityNodeInfo? = null): Boolean {
        val target = node ?: getAccessibilityNodeInfo() ?: return false
        val result = target.performAction(AccessibilityNodeInfo.ACTION_CLEAR_SELECTION)
        logger.d("Clear selection: $result")
        return result
    }

    /**
     * 展开节点
     */
    fun expandNode(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false
        val result = node.performAction(AccessibilityNodeInfo.ACTION_EXPAND)
        logger.d("Expand node: $result")
        return result
    }

    /**
     * 折叠节点
     */
    fun collapseNode(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false
        val result = node.performAction(AccessibilityNodeInfo.ACTION_COLLAPSE)
        logger.d("Collapse node: $result")
        return result
    }

    /**
     * 关闭弹窗/对话框
     */
    fun dismiss(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false
        val result = node.performAction(AccessibilityNodeInfo.ACTION_DISMISS)
        logger.d("Dismiss: $result")
        return result
    }

    /**
     * 设置焦点
     */
    fun focusNode(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false
        val result = node.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
        logger.d("Focus node: $result")
        return result
    }

    /**
     * 清除焦点
     */
    fun clearFocus(node: AccessibilityNodeInfo? = null): Boolean {
        val target = node ?: getAccessibilityNodeInfo() ?: return false
        val result = target.performAction(AccessibilityNodeInfo.ACTION_CLEAR_FOCUS)
        logger.d("Clear focus: $result")
        return result
    }

    // ==================== 剪贴板操作 ====================

    /**
     * 复制文本到剪贴板
     */
    fun copyToClipboard(text: String): Boolean {
        return try {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("auto_copy", text)
            clipboard.setPrimaryClip(clip)
            logger.d("Copied to clipboard: ${text.take(20)}...")
            true
        } catch (e: Exception) {
            logger.e("Copy to clipboard failed: ${e.message}")
            false
        }
    }

    /**
     * 从剪贴板获取文本
     */
    fun getClipboardText(): String? {
        return try {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = clipboard.primaryClip
            if (clip != null && clip.itemCount > 0) {
                clip.getItemAt(0).text?.toString()
            } else null
        } catch (e: Exception) {
            logger.e("Get clipboard failed: ${e.message}")
            null
        }
    }

    /**
     * 复制节点文本
     */
    fun copyNode(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false
        val result = node.performAction(AccessibilityNodeInfo.ACTION_COPY)
        logger.d("Copy node: $result")
        return result
    }

    /**
     * 剪切节点文本
     */
    fun cutNode(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false
        val result = node.performAction(AccessibilityNodeInfo.ACTION_CUT)
        logger.d("Cut node: $result")
        return result
    }

    /**
     * 粘贴到节点
     */
    fun pasteToNode(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false
        val result = node.performAction(AccessibilityNodeInfo.ACTION_PASTE)
        logger.d("Paste to node: $result")
        return result
    }

    // ==================== 节点查找 ====================

    /**
     * 按文本查找节点
     */
    fun findNodesByText(text: String, exact: Boolean = false): List<AccessibilityNodeInfo> {
        val root = getAccessibilityNodeInfo() ?: return emptyList()
        val result = mutableListOf<AccessibilityNodeInfo>()
        findNodesByTextRecursive(root, text, exact, result)
        return result
    }

    private fun findNodesByTextRecursive(node: AccessibilityNodeInfo, text: String, exact: Boolean, result: MutableList<AccessibilityNodeInfo>) {
        val nodeText = node.text?.toString() ?: ""
        val matches = if (exact) nodeText == text else nodeText.contains(text, ignoreCase = true)
        if (matches) {
            result.add(node)
        }
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { findNodesByTextRecursive(it, text, exact, result) }
        }
    }

    /**
     * 按内容描述查找节点
     */
    fun findNodesByContentDescription(description: String): List<AccessibilityNodeInfo> {
        val root = getAccessibilityNodeInfo() ?: return emptyList()
        val result = mutableListOf<AccessibilityNodeInfo>()
        findNodesByContentDescriptionRecursive(root, description, result)
        return result
    }

    private fun findNodesByContentDescriptionRecursive(node: AccessibilityNodeInfo, description: String, result: MutableList<AccessibilityNodeInfo>) {
        val nodeDesc = node.contentDescription?.toString() ?: ""
        if (nodeDesc.contains(description, ignoreCase = true)) {
            result.add(node)
        }
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { findNodesByContentDescriptionRecursive(it, description, result) }
        }
    }

    /**
     * 按ViewId查找节点
     */
    fun findNodeByViewId(viewId: String): AccessibilityNodeInfo? {
        val root = getAccessibilityNodeInfo() ?: return null
        val nodes = root.findAccessibilityNodeInfosByViewId(viewId)
        return nodes.firstOrNull()
    }

    /**
     * 查找可点击的节点
     */
    fun findClickableNodes(): List<AccessibilityNodeInfo> {
        val root = getAccessibilityNodeInfo() ?: return emptyList()
        val result = mutableListOf<AccessibilityNodeInfo>()
        findClickableNodesRecursive(root, result)
        return result
    }

    private fun findClickableNodesRecursive(node: AccessibilityNodeInfo, result: MutableList<AccessibilityNodeInfo>) {
        if (node.isClickable) {
            result.add(node)
        }
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { findClickableNodesRecursive(it, result) }
        }
    }

    /**
     * 查找可编辑的节点
     */
    fun findEditableNodes(): List<AccessibilityNodeInfo> {
        val root = getAccessibilityNodeInfo() ?: return emptyList()
        val result = mutableListOf<AccessibilityNodeInfo>()
        findEditableNodesRecursive(root, result)
        return result
    }

    private fun findEditableNodesRecursive(node: AccessibilityNodeInfo, result: MutableList<AccessibilityNodeInfo>) {
        if (node.isEditable) {
            result.add(node)
        }
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { findEditableNodesRecursive(it, result) }
        }
    }

    // ==================== 节点信息获取 ====================

    /**
     * 获取节点文本
     */
    fun getNodeText(node: AccessibilityNodeInfo?): String? {
        return node?.text?.toString()
    }

    /**
     * 获取节点内容描述
     */
    fun getNodeContentDescription(node: AccessibilityNodeInfo?): String? {
        return node?.contentDescription?.toString()
    }

    /**
     * 获取节点类名
     */
    fun getNodeClassName(node: AccessibilityNodeInfo?): String? {
        return node?.className?.toString()
    }

    /**
     * 获取节点ViewId
     */
    fun getNodeViewId(node: AccessibilityNodeInfo?): String? {
        return node?.viewIdResourceName
    }

    /**
     * 获取节点边界
     */
    fun getNodeBounds(node: AccessibilityNodeInfo?): Rect? {
        if (node == null) return null
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        return bounds
    }

    /**
     * 检查节点是否可见
     */
    fun isNodeVisible(node: AccessibilityNodeInfo?): Boolean {
        return node?.isVisibleToUser ?: false
    }

    /**
     * 检查节点是否启用
     */
    fun isNodeEnabled(node: AccessibilityNodeInfo?): Boolean {
        return node?.isEnabled ?: false
    }

    /**
     * 检查节点是否可滚动
     */
    fun isNodeScrollable(node: AccessibilityNodeInfo?): Boolean {
        return node?.isScrollable ?: false
    }

    /**
     * 检查节点是否可长按
     */
    fun isNodeLongClickable(node: AccessibilityNodeInfo?): Boolean {
        return node?.isLongClickable ?: false
    }

    /**
     * 检查节点是否可勾选
     */
    fun isNodeCheckable(node: AccessibilityNodeInfo?): Boolean {
        return node?.isCheckable ?: false
    }

    /**
     * 检查节点是否已勾选
     */
    fun isNodeChecked(node: AccessibilityNodeInfo?): Boolean {
        return node?.isChecked ?: false
    }

    // ==================== 辅助方法 ====================

    /**
     * 创建长按手势路径
     */
    private fun createLongClickPath(x: Float, y: Float, duration: Long): Path {
        return Path().apply {
            moveTo(x, y)
            // 长按就是停留在同一点
        }
    }

    /**
     * 分发多指手势
     */
    private suspend fun dispatchMultiFingerGesture(gesture: GestureDescription): Boolean = suspendCancellableCoroutine { continuation ->
        val result = dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                logger.d("Multi-finger gesture completed")
                continuation.resume(true)
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                logger.w("Multi-finger gesture cancelled")
                continuation.resume(false)
            }
        }, null)

        if (!result) {
            logger.w("Failed to dispatch multi-finger gesture")
            continuation.resume(false)
        }
    }
}
