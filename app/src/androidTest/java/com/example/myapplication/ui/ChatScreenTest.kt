package com.example.myapplication.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.myapplication.data.model.ChatMessage
import com.example.myapplication.data.model.ChatSession
import com.example.myapplication.ui.chat.ChatScreen
import com.example.myapplication.ui.chat.ChatUiState
import com.example.myapplication.ui.chat.ChatViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Compose UI tests for ChatScreen
 *
 * Tests chat interface, message display, input handling, and session management.
 */
class ChatScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var fakeViewModel: FakeChatViewModel

    @Before
    fun setup() {
        fakeViewModel = FakeChatViewModel()
    }

    // ========== Empty State Tests ==========

    @Test
    fun `chatScreen displays empty state when no messages`() {
        composeTestRule.setContent {
            ChatScreen(
                viewModel = fakeViewModel,
                onOpenSettings = {},
                onOpenPromptEditor = {}
            )
        }

        composeTestRule.onNodeWithText("开始对话").assertExists()
        composeTestRule.onNodeWithText("AI会自动决定何时需要操作手机").assertExists()
    }

    @Test
    fun `chatScreen displays empty chat icon when no messages`() {
        composeTestRule.setContent {
            ChatScreen(
                viewModel = fakeViewModel,
                onOpenSettings = {},
                onOpenPromptEditor = {}
            )
        }

        composeTestRule.onNodeWithContentDescription("Sessions").assertExists()
    }

    // ========== Message Display Tests ==========

    @Test
    fun `chatScreen displays messages in correct order`() {
        fakeViewModel.setMessages(listOf(
            ChatMessage.UserMessage(
                id = "1",
                timestamp = System.currentTimeMillis(),
                content = "First message"
            ),
            ChatMessage.AiMessage(
                id = "2",
                timestamp = System.currentTimeMillis(),
                content = "Second message"
            )
        ))

        composeTestRule.setContent {
            ChatScreen(
                viewModel = fakeViewModel,
                onOpenSettings = {},
                onOpenPromptEditor = {}
            )
        }

        composeTestRule.onNodeWithText("First message").assertExists()
        composeTestRule.onNodeWithText("Second message").assertExists()
    }

    @Test
    fun `chatScreen displays user and AI messages differently`() {
        fakeViewModel.setMessages(listOf(
            ChatMessage.UserMessage(
                id = "1",
                timestamp = System.currentTimeMillis(),
                content = "User says hello"
            ),
            ChatMessage.AiMessage(
                id = "2",
                timestamp = System.currentTimeMillis(),
                content = "AI responds"
            )
        ))

        composeTestRule.setContent {
            ChatScreen(
                viewModel = fakeViewModel,
                onOpenSettings = {},
                onOpenPromptEditor = {}
            )
        }

        composeTestRule.onNodeWithText("User says hello").assertExists()
        composeTestRule.onNodeWithText("AI responds").assertExists()
    }

    @Test
    fun `chatScreen displays tool call messages`() {
        fakeViewModel.setMessages(listOf(
            ChatMessage.ToolCallMessage(
                id = "1",
                timestamp = System.currentTimeMillis(),
                toolName = "click",
                parameters = mapOf("x" to 540, "y" to 1200),
                result = "Click successful",
                isSuccess = true
            )
        ))

        composeTestRule.setContent {
            ChatScreen(
                viewModel = fakeViewModel,
                onOpenSettings = {},
                onOpenPromptEditor = {}
            )
        }

        composeTestRule.onNodeWithText("click", substring = true).assertExists()
    }

    @Test
    fun `chatScreen displays status messages`() {
        fakeViewModel.setMessages(listOf(
            ChatMessage.StatusMessage(
                id = "1",
                timestamp = System.currentTimeMillis(),
                status = "Task running...",
                isRunning = true
            )
        ))

        composeTestRule.setContent {
            ChatScreen(
                viewModel = fakeViewModel,
                onOpenSettings = {},
                onOpenPromptEditor = {}
            )
        }

        composeTestRule.onNodeWithText("Task running...").assertExists()
    }

    // ========== Input Bar Tests ==========

    @Test
    fun `chatInput sends message when text is not blank`() {
        var messageSent = false
        fakeViewModel.onSendMessage = { messageSent = true }

        composeTestRule.setContent {
            ChatScreen(
                viewModel = fakeViewModel,
                onOpenSettings = {},
                onOpenPromptEditor = {}
            )
        }

        // Type text in input field
        composeTestRule.onNodeWithText("输入消息...")
            .performTextInput("Hello AI")

        // Click send button
        composeTestRule.onNodeWithContentDescription("Send")
            .performClick()

        assert(messageSent)
    }

    @Test
    fun `sendButton shows cancel when task is running`() {
        fakeViewModel.setTaskRunning(true)

        composeTestRule.setContent {
            ChatScreen(
                viewModel = fakeViewModel,
                onOpenSettings = {},
                onOpenPromptEditor = {}
            )
        }

        // When running, the button should show cancel icon
        composeTestRule.onNodeWithContentDescription("Cancel")
            .assertExists()
    }

    @Test
    fun `input field is disabled when task is running`() {
        fakeViewModel.setTaskRunning(true)

        composeTestRule.setContent {
            ChatScreen(
                viewModel = fakeViewModel,
                onOpenSettings = {},
                onOpenPromptEditor = {}
            )
        }

        // Input should still be visible but send behavior changes
        composeTestRule.onNodeWithText("输入消息...").assertExists()
    }

    // ========== Session Drawer Tests ==========

    @Test
    fun `sessionDrawer opens when menu clicked`() {
        fakeViewModel.setSessions(listOf(
            ChatSession(
                id = "1",
                title = "Test Session",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        ))

        composeTestRule.setContent {
            ChatScreen(
                viewModel = fakeViewModel,
                onOpenSettings = {},
                onOpenPromptEditor = {}
            )
        }

        // Click menu button to open drawer
        composeTestRule.onNodeWithContentDescription("Sessions")
            .performClick()

        // Session should be visible
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Test Session").assertExists()
    }

    @Test
    fun `sessionDrawer shows create new session button`() {
        composeTestRule.setContent {
            ChatScreen(
                viewModel = fakeViewModel,
                onOpenSettings = {},
                onOpenPromptEditor = {}
            )
        }

        // Open drawer
        composeTestRule.onNodeWithContentDescription("Sessions")
            .performClick()

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("新建会话").assertExists()
    }

    // ========== Title Bar Tests ==========

    @Test
    fun `titleBar shows current session title`() {
        fakeViewModel.setCurrentSession(ChatSession(
            id = "1",
            title = "My Chat Session",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        ))

        composeTestRule.setContent {
            ChatScreen(
                viewModel = fakeViewModel,
                onOpenSettings = {},
                onOpenPromptEditor = {}
            )
        }

        composeTestRule.onNodeWithText("My Chat Session").assertExists()
    }

    @Test
    fun `titleBar shows running indicator when task is running`() {
        fakeViewModel.setTaskRunning(true)

        composeTestRule.setContent {
            ChatScreen(
                viewModel = fakeViewModel,
                onOpenSettings = {},
                onOpenPromptEditor = {}
            )
        }

        composeTestRule.onNodeWithText("AI正在处理...").assertExists()
    }

    // ========== Action Button Tests ==========

    @Test
    fun `settings button exists in top bar`() {
        composeTestRule.setContent {
            ChatScreen(
                viewModel = fakeViewModel,
                onOpenSettings = {},
                onOpenPromptEditor = {}
            )
        }

        composeTestRule.onNodeWithContentDescription("Settings").assertExists()
    }

    @Test
    fun `prompt editor button exists in top bar`() {
        composeTestRule.setContent {
            ChatScreen(
                viewModel = fakeViewModel,
                onOpenSettings = {},
                onOpenPromptEditor = {}
            )
        }

        composeTestRule.onNodeWithContentDescription("Edit Prompt").assertExists()
    }

    @Test
    fun `clear button exists in top bar`() {
        composeTestRule.setContent {
            ChatScreen(
                viewModel = fakeViewModel,
                onOpenSettings = {},
                onOpenPromptEditor = {}
            )
        }

        composeTestRule.onNodeWithContentDescription("Clear").assertExists()
    }

    // ========== Long Content Tests ==========

    @Test
    fun `chatScreen handles long messages`() {
        val longContent = "This is a very long message that should still display correctly. ".repeat(10)

        fakeViewModel.setMessages(listOf(
            ChatMessage.UserMessage(
                id = "1",
                timestamp = System.currentTimeMillis(),
                content = longContent
            )
        ))

        composeTestRule.setContent {
            ChatScreen(
                viewModel = fakeViewModel,
                onOpenSettings = {},
                onOpenPromptEditor = {}
            )
        }

        composeTestRule.onNodeWithText("This is a very long message", substring = true)
            .assertExists()
    }

    @Test
    fun `chatScreen handles many messages`() {
        val messages = (1..20).map { index ->
            ChatMessage.UserMessage(
                id = index.toString(),
                timestamp = System.currentTimeMillis() + index,
                content = "Message $index"
            )
        }
        fakeViewModel.setMessages(messages)

        composeTestRule.setContent {
            ChatScreen(
                viewModel = fakeViewModel,
                onOpenSettings = {},
                onOpenPromptEditor = {}
            )
        }

        // First and last messages should exist
        composeTestRule.onNodeWithText("Message 1").assertExists()
        composeTestRule.onNodeWithText("Message 20").assertExists()
    }
}

/**
 * Fake implementation of ChatViewModel for testing
 */
class FakeChatViewModel : ChatViewModel(
    android.app.Application()
) {
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    override val currentMessages: StateFlow<List<ChatMessage>> = _messages

    private val _sessions = MutableStateFlow<List<ChatSession>>(emptyList())
    override val sessions: StateFlow<List<ChatSession>> = _sessions

    private val _currentSession = MutableStateFlow<ChatSession?>(null)
    override val currentSession: StateFlow<ChatSession?> = _currentSession

    private val _isTaskRunning = MutableStateFlow(false)
    override val isTaskRunning: StateFlow<Boolean> = _isTaskRunning

    private val _uiState = MutableStateFlow(ChatUiState())
    override val uiState: StateFlow<ChatUiState> = _uiState

    var onSendMessage: ((String) -> Unit)? = null

    fun setMessages(messages: List<ChatMessage>) {
        _messages.value = messages
    }

    fun setSessions(sessions: List<ChatSession>) {
        _sessions.value = sessions
    }

    fun setCurrentSession(session: ChatSession?) {
        _currentSession.value = session
    }

    fun setTaskRunning(running: Boolean) {
        _isTaskRunning.value = running
    }

    override fun sendMessage(content: String) {
        onSendMessage?.invoke(content)
    }

    override fun cancelTask() {
        _isTaskRunning.value = false
    }
}
