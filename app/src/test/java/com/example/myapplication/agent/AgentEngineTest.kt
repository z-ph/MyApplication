package com.example.myapplication.agent

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Unit tests for AgentEngine
 *
 * Tests agent action descriptions.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AgentEngineTest {

    // ========== AgentAction Tests ==========

    @Test
    fun `AgentAction Click should have correct description`() {
        val action = AgentAction.Click(100f, 200f)
        assertThat(action.toDescription()).isEqualTo("Click(100, 200)")
    }

    @Test
    fun `AgentAction Swipe should have correct description`() {
        val action = AgentAction.Swipe("up", 500)
        assertThat(action.toDescription()).isEqualTo("Swipe(up, 500)")
    }

    @Test
    fun `AgentAction Type should truncate long text`() {
        val action = AgentAction.Type("This is a very long text that should be truncated")
        assertThat(action.toDescription()).contains("Type(\"")
    }

    @Test
    fun `AgentAction Finish should include summary`() {
        val action = AgentAction.Finish("Task completed successfully")
        assertThat(action.toDescription()).isEqualTo("Finish(\"Task completed successfully\")")
    }

    @Test
    fun `AgentAction Unknown should include name`() {
        val action = AgentAction.Unknown("custom_action")
        assertThat(action.toDescription()).isEqualTo("Unknown(custom_action)")
    }

    @Test
    fun `AgentAction Home should have correct description`() {
        val action = AgentAction.Home
        assertThat(action.toDescription()).isEqualTo("Home()")
    }

    @Test
    fun `AgentAction Back should have correct description`() {
        val action = AgentAction.Back
        assertThat(action.toDescription()).isEqualTo("Back()")
    }

    @Test
    fun `AgentAction Wait should have correct description`() {
        val action = AgentAction.Wait(1000)
        assertThat(action.toDescription()).isEqualTo("Wait(1000ms)")
    }

    @Test
    fun `AgentAction Reply should have correct description`() {
        val action = AgentAction.Reply("Hello")
        assertThat(action.toDescription()).isEqualTo("Reply(\"Hello\")")
    }

    @Test
    fun `AgentAction ClickByText should have correct description`() {
        val action = AgentAction.ClickByText("button", true)
        assertThat(action.toDescription()).isEqualTo("ClickByText(\"button\", exact=true)")
    }

    @Test
    fun `AgentAction LongClick should have correct description`() {
        val action = AgentAction.LongClick(100f, 200f, 500)
        assertThat(action.toDescription()).isEqualTo("LongClick(100, 200, 500ms)")
    }

    @Test
    fun `AgentAction Drag should have correct description`() {
        val action = AgentAction.Drag(100f, 200f, 300f, 400f)
        assertThat(action.toDescription()).isEqualTo("Drag(100,200 -> 300,400)")
    }

    @Test
    fun `AgentAction OpenApp should have correct description`() {
        val action = AgentAction.OpenApp("com.example.app")
        assertThat(action.toDescription()).isEqualTo("OpenApp(com.example.app)")
    }

    @Test
    fun `AgentAction ScrollForward should have correct description`() {
        val action = AgentAction.ScrollForward
        assertThat(action.toDescription()).isEqualTo("ScrollForward()")
    }

    @Test
    fun `AgentAction LockScreen should have correct description`() {
        val action = AgentAction.LockScreen
        assertThat(action.toDescription()).isEqualTo("LockScreen()")
    }

    @Test
    fun `AgentAction OpenNotifications should have correct description`() {
        val action = AgentAction.OpenNotifications
        assertThat(action.toDescription()).isEqualTo("OpenNotifications()")
    }
}
