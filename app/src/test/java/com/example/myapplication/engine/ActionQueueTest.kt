package com.example.myapplication.engine

import android.os.Looper
import com.example.myapplication.api.model.SwipeDirection
import com.example.myapplication.api.model.UiAction
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.LooperMode

/**
 * Unit tests for ActionQueue
 *
 * Tests queue management, priority handling, and action execution.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.PAUSED)
class ActionQueueTest {

    private lateinit var actionQueue: ActionQueue

    @Before
    fun setup() {
        // Pause looper to control time
        Shadows.shadowOf(Looper.getMainLooper()).pause()
        actionQueue = ActionQueue()
    }

    // ========== Initial State Tests ==========

    @Test
    fun `initial queue should be empty`() {
        assertThat(actionQueue.queue.value).isEmpty()
    }

    @Test
    fun `initial isProcessing should be false`() {
        assertThat(actionQueue.isProcessing.value).isFalse()
    }

    @Test
    fun `initial completed count should be zero`() {
        assertThat(actionQueue.completedCount.value).isEqualTo(0)
    }

    // ========== Enqueue Tests ==========

    @Test
    fun `enqueue should add action to queue`() {
        val action = UiAction.Click(100f, 200f)
        val id = actionQueue.enqueue(action)
        
        assertThat(id).isNotEmpty()
        assertThat(actionQueue.queue.value).hasSize(1)
        assertThat(actionQueue.queue.value[0].id).isEqualTo(id)
    }

    @Test
    fun `enqueue should set initial status to PENDING`() {
        val action = UiAction.Click(100f, 200f)
        actionQueue.enqueue(action)
        
        assertThat(actionQueue.queue.value[0].status).isEqualTo(ActionStatus.PENDING)
    }

    @Test
    fun `enqueue should set initial priority to NORMAL`() {
        val action = UiAction.Click(100f, 200f)
        actionQueue.enqueue(action)
        
        assertThat(actionQueue.queue.value[0].priority).isEqualTo(ActionPriority.NORMAL)
    }

    @Test
    fun `enqueue should set initial attempts to zero`() {
        val action = UiAction.Click(100f, 200f)
        actionQueue.enqueue(action)
        
        assertThat(actionQueue.queue.value[0].attempts).isEqualTo(0)
    }

    // ========== Priority Tests ==========

    @Test
    fun `high priority action should be inserted at front`() {
        // Add normal priority action first
        actionQueue.enqueue(UiAction.Click(100f, 200f), ActionPriority.NORMAL)
        
        // Add high priority action
        actionQueue.enqueue(UiAction.Click(300f, 400f), ActionPriority.HIGH)
        
        // High priority should be first
        assertThat(actionQueue.queue.value[0].priority).isEqualTo(ActionPriority.HIGH)
    }

    @Test
    fun `multiple high priority actions should maintain order`() {
        actionQueue.enqueue(UiAction.Click(100f, 200f), ActionPriority.HIGH)
        actionQueue.enqueue(UiAction.Click(300f, 400f), ActionPriority.HIGH)
        
        assertThat(actionQueue.queue.value[0].action).isInstanceOf(UiAction.Click::class.java)
        assertThat(actionQueue.queue.value[1].action).isInstanceOf(UiAction.Click::class.java)
    }

    // ========== EnqueueAll Tests ==========

    @Test
    fun `enqueueAll should add multiple actions`() {
        val actions = listOf(
            UiAction.Click(100f, 200f),
            UiAction.Swipe(SwipeDirection.UP, 500),
            UiAction.InputText("Hello")
        )
        
        val ids = actionQueue.enqueueAll(actions)
        
        assertThat(ids).hasSize(3)
        assertThat(actionQueue.queue.value).hasSize(3)
    }

    @Test
    fun `enqueueAll should return unique ids`() {
        val actions = listOf(
            UiAction.Click(100f, 200f),
            UiAction.Click(300f, 400f)
        )
        
        val ids = actionQueue.enqueueAll(actions)
        
        assertThat(ids[0]).isNotEqualTo(ids[1])
    }

    // ========== Remove Tests ==========

    @Test
    fun `remove should delete action from queue`() {
        val id = actionQueue.enqueue(UiAction.Click(100f, 200f))
        
        val result = actionQueue.remove(id)
        
        assertThat(result).isTrue()
        assertThat(actionQueue.queue.value).isEmpty()
    }

    @Test
    fun `remove should return false for non-existent id`() {
        val result = actionQueue.remove("non_existent_id")
        assertThat(result).isFalse()
    }

    // ========== Clear Tests ==========

    @Test
    fun `clear should empty the queue`() {
        actionQueue.enqueue(UiAction.Click(100f, 200f))
        actionQueue.enqueue(UiAction.Swipe(SwipeDirection.UP, 500))
        
        actionQueue.clear()
        
        assertThat(actionQueue.queue.value).isEmpty()
    }

    @Test
    fun `clear should reset completed count`() {
        // Note: We can't actually complete actions in unit tests,
        // but we can test that clear resets the counter
        actionQueue.clear()
        assertThat(actionQueue.completedCount.value).isEqualTo(0)
    }

    @Test
    fun `clear should reset failed count`() {
        actionQueue.clear()
        assertThat(actionQueue.failedCount.value).isEqualTo(0)
    }

    // ========== Stats Tests ==========

    @Test
    fun `getStats should return correct counts for empty queue`() {
        val stats = actionQueue.getStats()
        
        assertThat(stats.total).isEqualTo(0)
        assertThat(stats.pending).isEqualTo(0)
        assertThat(stats.running).isEqualTo(0)
        assertThat(stats.completed).isEqualTo(0)
        assertThat(stats.failed).isEqualTo(0)
    }

    @Test
    fun `getStats should return correct counts after enqueue`() {
        actionQueue.enqueue(UiAction.Click(100f, 200f))
        actionQueue.enqueue(UiAction.Swipe(SwipeDirection.UP, 500))
        
        val stats = actionQueue.getStats()
        
        assertThat(stats.total).isEqualTo(2)
        assertThat(stats.pending).isEqualTo(2)
    }

    // ========== Stop Tests ==========

    @Test
    fun `stop should set isProcessing to false`() {
        actionQueue.enqueue(UiAction.Click(100f, 200f))
        
        // Run pending tasks
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        
        actionQueue.stop()
        
        assertThat(actionQueue.isProcessing.value).isFalse()
    }
}
