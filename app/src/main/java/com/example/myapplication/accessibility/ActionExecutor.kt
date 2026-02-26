package com.example.myapplication.accessibility

import com.example.myapplication.api.model.NavigationAction
import com.example.myapplication.api.model.SwipeDirection
import com.example.myapplication.api.model.UiAction
import com.example.myapplication.utils.Logger
import kotlinx.coroutines.delay

/**
 * Action Executor
 * Executes UI actions using the accessibility service
 */
class ActionExecutor {

    companion object {
        private const val TAG = "ActionExecutor"

        // Default delays between actions
        private const val DEFAULT_ACTION_DELAY = 300L
        private const val CLICK_DELAY = 200L
        private const val SWIPE_DELAY = 400L
        private const val NAVIGATION_DELAY = 500L
    }

    private val logger = Logger(TAG)

    /**
     * Execute a single action
     *
     * @param action The action to execute
     * @return true if action was executed successfully
     */
    suspend fun executeAction(action: UiAction): Boolean {
        val service = AutoService.getInstance()
        if (service == null) {
            logger.e("Accessibility service not available")
            return false
        }

        return when (action) {
            is UiAction.Click -> {
                logger.d("Executing click at (${action.x}, ${action.y})")
                service.click(action.x, action.y).also {
                    if (it) delay(CLICK_DELAY)
                }
            }
            is UiAction.Swipe -> {
                logger.d("Executing swipe ${action.direction} distance ${action.distance}")
                service.swipe(
                    direction = action.direction,
                    distance = action.distance
                ).also {
                    if (it) delay(SWIPE_DELAY)
                }
            }
            is UiAction.InputText -> {
                logger.d("Executing text input: ${action.text}")
                service.inputText(action.text).also {
                    if (it) delay(CLICK_DELAY)
                }
            }
            is UiAction.ClickNode -> {
                logger.d("Executing click on node: ${action.nodeId}")
                val node = findNodeById(service, action.nodeId)
                if (node != null) {
                    service.clickNodeAsync(node).also {
                        if (it) delay(CLICK_DELAY)
                    }
                } else {
                    logger.w("Node not found: ${action.nodeId}")
                    false
                }
            }
            is UiAction.Navigate -> {
                logger.d("Executing navigation: ${action.action}")
                val result = when (action.action) {
                    NavigationAction.BACK -> service.pressBack()
                    NavigationAction.HOME -> service.pressHome()
                    NavigationAction.RECENTS -> service.pressRecents()
                }
                if (result) delay(NAVIGATION_DELAY)
                result
            }
            is UiAction.Wait -> {
                logger.d("Waiting ${action.durationMs}ms")
                delay(action.durationMs)
                true
            }
        }
    }

    /**
     * Execute a list of actions in sequence
     *
     * @param actions List of actions to execute
     * @param onProgress Callback for progress updates (current, total)
     * @return true if all actions executed successfully
     */
    suspend fun executeActions(
        actions: List<UiAction>,
        onProgress: ((Int, Int) -> Unit)? = null
    ): ActionResult {
        if (actions.isEmpty()) {
            logger.d("No actions to execute")
            return ActionResult.Empty
        }

        logger.d("Executing ${actions.size} actions")
        val results = mutableListOf<ActionResult>()
        var successCount = 0
        var failureCount = 0

        actions.forEachIndexed { index, action ->
            onProgress?.invoke(index + 1, actions.size)

            val result = executeAction(action)
            if (result) {
                successCount++
                results.add(ActionResult.Success(action))
            } else {
                failureCount++
                results.add(ActionResult.Failed(action, "Execution failed"))
            }

            // Small delay between actions
            if (index < actions.size - 1) {
                delay(DEFAULT_ACTION_DELAY)
            }
        }

        logger.d("Execution complete: $successCount success, $failureCount failed")

        return if (failureCount == 0) {
            ActionResult.AllSuccess(successCount)
        } else if (successCount == 0) {
            ActionResult.AllFailed(failureCount)
        } else {
            ActionResult.PartialSuccess(successCount, failureCount)
        }
    }

    /**
     * Execute a batch of actions and wait for completion
     */
    suspend fun executeBatch(actions: List<UiAction>): ActionResult {
        return executeActions(actions)
    }

    /**
     * Execute action with retry
     */
    suspend fun executeWithRetry(
        action: UiAction,
        maxRetries: Int = 3,
        retryDelay: Long = 500L
    ): Boolean {
        repeat(maxRetries) { attempt ->
            val result = executeAction(action)
            if (result) {
                logger.d("Action succeeded on attempt ${attempt + 1}")
                return true
            }

            if (attempt < maxRetries - 1) {
                logger.d("Action failed, retrying in ${retryDelay}ms...")
                delay(retryDelay)
            }
        }

        logger.w("Action failed after $maxRetries attempts")
        return false
    }

    /**
     * Find a node by its view ID resource name
     */
    private fun findNodeById(service: AutoService, nodeId: String): android.view.accessibility.AccessibilityNodeInfo? {
        val rootNode = service.getAccessibilityNodeInfo() ?: return null

        return findNodeByIdRecursive(rootNode, nodeId)
    }

    private fun findNodeByIdRecursive(
        node: android.view.accessibility.AccessibilityNodeInfo,
        nodeId: String
    ): android.view.accessibility.AccessibilityNodeInfo? {
        // Check current node
        val viewId = node.viewIdResourceName
        if (viewId != null && viewId.contains(nodeId)) {
            return node
        }

        // Check text content
        val text = node.text?.toString() ?: node.contentDescription?.toString()
        if (text == nodeId) {
            return node
        }

        // Recursively search children
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findNodeByIdRecursive(child, nodeId)
            if (result != null) {
                return result
            }
        }

        return null
    }
}

/**
 * Action execution result
 */
sealed class ActionResult {
    object Empty : ActionResult()
    data class Success(val action: UiAction) : ActionResult()
    data class Failed(val action: UiAction, val reason: String) : ActionResult()
    data class AllSuccess(val count: Int) : ActionResult()
    data class AllFailed(val count: Int) : ActionResult()
    data class PartialSuccess(val successCount: Int, val failureCount: Int) : ActionResult()
}
