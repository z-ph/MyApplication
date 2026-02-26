package com.example.myapplication.agent

import com.example.myapplication.agent.models.ToolCallInfo
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for ContextManager
 *
 * Tests message history management, image restrictions, and summarization logic.
 */
class ContextManagerTest {

    private lateinit var contextManager: ContextManager

    @Before
    fun setup() {
        contextManager = ContextManager()
    }

    // ========== buildMessages Tests ==========

    @Test
    fun `buildMessages should include system prompt`() {
        val systemPrompt = "You are a helpful assistant"
        val messages = contextManager.buildMessages(systemPrompt, "Hello")

        assertThat(messages).isNotEmpty()
        assertThat(messages[0]["role"]).isEqualTo("system")
        assertThat(messages[0]["content"]).isEqualTo(systemPrompt)
    }

    @Test
    fun `buildMessages should include user goal`() {
        val goal = "Click on the button"
        val messages = contextManager.buildMessages("System prompt", goal)

        val lastMessage = messages.last()
        assertThat(lastMessage["role"]).isEqualTo("user")
        @Suppress("UNCHECKED_CAST")
        val content = lastMessage["content"]
        if (content is String) {
            assertThat(content).isEqualTo(goal)
        }
    }

    @Test
    fun `buildMessages should include image when provided`() {
        val goal = "Analyze this screen"
        val base64Image = "fakeBase64ImageData"

        val messages = contextManager.buildMessages("System", goal, base64Image)

        val lastMessage = messages.last()
        assertThat(lastMessage["role"]).isEqualTo("user")
        @Suppress("UNCHECKED_CAST")
        val content = lastMessage["content"] as? List<Map<String, Any>>
        assertThat(content).isNotNull()
        assertThat(content!!.any { it["type"] == "image_url" }).isTrue()
    }

    // ========== addUserMessage Tests ==========

    @Test
    fun `addUserMessage should increase message count`() {
        assertThat(contextManager.getMessageCount()).isEqualTo(0)

        contextManager.addUserMessage("Hello")
        assertThat(contextManager.getMessageCount()).isEqualTo(1)

        contextManager.addUserMessage("World")
        assertThat(contextManager.getMessageCount()).isEqualTo(2)
    }

    @Test
    fun `addUserMessage with image should increase image count`() {
        assertThat(contextManager.getImageCount()).isEqualTo(0)

        contextManager.addUserMessage("With image", hasImage = true)
        assertThat(contextManager.getImageCount()).isEqualTo(1)

        contextManager.addUserMessage("Without image", hasImage = false)
        assertThat(contextManager.getImageCount()).isEqualTo(1)
    }

    // ========== addAssistantMessage Tests ==========

    @Test
    fun `addAssistantMessage should increase message count`() {
        contextManager.addAssistantMessage("I will help you")
        assertThat(contextManager.getMessageCount()).isEqualTo(1)
    }

    @Test
    fun `addAssistantMessage with tool calls should include them`() {
        val toolCalls = listOf(
            ToolCallInfo(
                id = "call_1",
                name = "click",
                parameters = mapOf("x" to 540, "y" to 1200),
                rawMatch = "click(540, 1200)"
            )
        )

        contextManager.addAssistantMessage("I will click the button", toolCalls)

        val messages = contextManager.buildMessages("System", "Goal")
        val assistantMessage = messages.find { it["role"] == "assistant" }

        assertThat(assistantMessage).isNotNull()
        assertThat(assistantMessage!!.containsKey("tool_calls")).isTrue()
    }

    // ========== addToolResult Tests ==========

    @Test
    fun `addToolResult should add tool message`() {
        contextManager.addToolResult("call_1", "Click successful")

        assertThat(contextManager.getMessageCount()).isEqualTo(1)
    }

    @Test
    fun `addToolResult message should have tool role`() {
        contextManager.addToolResult("call_1", "Success")

        val messages = contextManager.buildMessages("System", "Goal")
        val toolMessage = messages.find { it["role"] == "tool" }

        assertThat(toolMessage).isNotNull()
        assertThat(toolMessage!!["tool_call_id"]).isEqualTo("call_1")
        assertThat(toolMessage["content"]).isEqualTo("Success")
    }

    // ========== clear Tests ==========

    @Test
    fun `clear should reset all state`() {
        contextManager.addUserMessage("Hello", hasImage = true)
        contextManager.addAssistantMessage("Hi there")
        contextManager.addToolResult("call_1", "Done")

        assertThat(contextManager.getMessageCount()).isEqualTo(3)
        assertThat(contextManager.getImageCount()).isEqualTo(1)

        contextManager.clear()

        assertThat(contextManager.getMessageCount()).isEqualTo(0)
        assertThat(contextManager.getImageCount()).isEqualTo(0)
    }

    @Test
    fun `clear should allow adding messages after clear`() {
        contextManager.addUserMessage("First")
        contextManager.clear()
        contextManager.addUserMessage("Second")

        assertThat(contextManager.getMessageCount()).isEqualTo(1)
    }

    // ========== needsSummarization Tests ==========

    @Test
    fun `needsSummarization returns false for few messages`() {
        repeat(5) {
            contextManager.addUserMessage("Message $it")
        }

        assertThat(contextManager.needsSummarization()).isFalse()
    }

    @Test
    fun `needsSummarization returns true when threshold reached`() {
        // Add SUMMARY_THRESHOLD (20) messages
        repeat(20) {
            contextManager.addUserMessage("Message $it")
        }

        assertThat(contextManager.needsSummarization()).isTrue()
    }

    // ========== summarizeIfNeeded Tests ==========

    @Test
    fun `summarizeIfNeeded returns false when not needed`() {
        contextManager.addUserMessage("Hello")

        val result = contextManager.summarizeIfNeeded()

        assertThat(result).isFalse()
    }

    @Test
    fun `summarizeIfNeeded returns true and reduces message count`() {
        // Add many messages to trigger summarization
        repeat(25) {
            contextManager.addUserMessage("User message $it")
            contextManager.addAssistantMessage("Assistant response $it")
        }

        val initialCount = contextManager.getMessageCount()
        val result = contextManager.summarizeIfNeeded()

        assertThat(result).isTrue()
        assertThat(contextManager.getMessageCount()).isLessThan(initialCount)
    }

    // ========== Integration Tests ==========

    @Test
    fun `buildMessages after multiple interactions should maintain order`() {
        contextManager.addUserMessage("First user message")
        contextManager.addAssistantMessage("First assistant message")
        contextManager.addToolResult("call_1", "Tool result")

        val messages = contextManager.buildMessages("System", "Final goal")

        // System + 3 history + final user = 5 messages
        assertThat(messages).hasSize(5)

        // Verify order
        assertThat(messages[0]["role"]).isEqualTo("system")
        assertThat(messages[1]["role"]).isEqualTo("user")
        assertThat(messages[2]["role"]).isEqualTo("assistant")
        assertThat(messages[3]["role"]).isEqualTo("tool")
        assertThat(messages[4]["role"]).isEqualTo("user")
    }

    @Test
    fun `getMessagesForDebug should return all messages`() {
        contextManager.addUserMessage("Test message")

        val debugMessages = contextManager.getMessagesForDebug()

        assertThat(debugMessages).hasSize(1)
        assertThat(debugMessages[0].role).isEqualTo("user")
        assertThat(debugMessages[0].content).isEqualTo("Test message")
    }
}
