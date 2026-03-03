package com.example.myapplication.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.myapplication.data.local.AppDatabase
import com.example.myapplication.data.local.dao.MessageDao
import com.example.myapplication.data.local.dao.SessionDao
import com.example.myapplication.data.model.ChatMessage
import com.example.myapplication.data.model.ChatSession
import com.example.myapplication.data.repository.ChatRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.UUID

/**
 * Unit tests for ChatRepository
 *
 * Tests session and message CRUD operations using an in-memory database.
 * Uses Robolectric to provide Android context.
 */
@RunWith(RobolectricTestRunner::class)
class ChatRepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var sessionDao: SessionDao
    private lateinit var messageDao: MessageDao
    private lateinit var repository: ChatRepository

    @Before
    fun setup() {
        // Note: ChatRepository now uses singleton pattern with private constructor
        // These tests need to be rewritten to use the singleton instance
        // For now, this test class is disabled
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        ).allowMainThreadQueries().build()

        sessionDao = database.sessionDao()
        messageDao = database.messageDao()

        // Use singleton instance for testing
        repository = ChatRepository.getInstance(context)
    }

    @After
    fun tearDown() {
        database.close()
    }

    // ========== createSession Tests ==========

    @Test
    fun `createSession should return session with valid id`() = runTest {
        val session = repository.createSession("Test Session")

        assertThat(session.id).isNotEmpty()
        assertThat(UUID.fromString(session.id)).isNotNull() // Valid UUID
    }

    @Test
    fun `createSession should set timestamps`() = runTest {
        val before = System.currentTimeMillis()
        val session = repository.createSession("Test")
        val after = System.currentTimeMillis()

        assertThat(session.createdAt).isAtLeast(before)
        assertThat(session.createdAt).isAtMost(after)
        assertThat(session.updatedAt).isEqualTo(session.createdAt)
    }

    @Test
    fun `createSession should use default title when not provided`() = runTest {
        val session = repository.createSession()

        assertThat(session.title).isEqualTo("New Chat")
    }

    @Test
    fun `createSession should persist to database`() = runTest {
        val session = repository.createSession("Persisted Session")

        val retrieved = repository.getSessionById(session.id)
        assertThat(retrieved).isNotNull()
        assertThat(retrieved!!.title).isEqualTo("Persisted Session")
    }

    // ========== addMessage Tests ==========

    @Test
    fun `addMessage should persist message to database`() = runTest {
        val session = repository.createSession("Test")
        val message = ChatMessage.UserMessage(
            id = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            content = "Hello, World!"
        )

        repository.addMessage(session.id, message)

        val messages = repository.getMessagesForSession(session.id).first()
        assertThat(messages).hasSize(1)
        assertThat((messages[0] as ChatMessage.UserMessage).content).isEqualTo("Hello, World!")
    }

    @Test
    fun `addMessage should maintain message order`() = runTest {
        val session = repository.createSession("Test")

        repository.addMessage(session.id, ChatMessage.UserMessage(
            id = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            content = "First"
        ))
        repository.addMessage(session.id, ChatMessage.AiMessage(
            id = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            content = "Second"
        ))
        repository.addMessage(session.id, ChatMessage.UserMessage(
            id = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            content = "Third"
        ))

        val messages = repository.getMessagesForSession(session.id).first()
        assertThat(messages).hasSize(3)
        assertThat((messages[0] as ChatMessage.UserMessage).content).isEqualTo("First")
        assertThat((messages[1] as ChatMessage.AiMessage).content).isEqualTo("Second")
        assertThat((messages[2] as ChatMessage.UserMessage).content).isEqualTo("Third")
    }

    @Test
    fun `addMessage should update session timestamp`() = runTest {
        val session = repository.createSession("Test")
        val originalUpdatedAt = session.updatedAt

        Thread.sleep(10) // Ensure time difference
        repository.addMessage(session.id, ChatMessage.UserMessage(
            id = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            content = "New message"
        ))

        val updatedSession = repository.getSessionById(session.id)
        assertThat(updatedSession!!.updatedAt).isGreaterThan(originalUpdatedAt)
    }

    // ========== getMessagesForSession Tests ==========

    @Test
    fun `getMessagesForSession should return messages in order`() = runTest {
        val session = repository.createSession("Test")

        repository.addMessage(session.id, ChatMessage.UserMessage(
            id = "msg1", timestamp = 100, content = "A"
        ))
        repository.addMessage(session.id, ChatMessage.AiMessage(
            id = "msg2", timestamp = 200, content = "B"
        ))

        val messages = repository.getMessagesForSession(session.id).first()

        assertThat(messages).hasSize(2)
        assertThat(messages[0].id).isEqualTo("msg1")
        assertThat(messages[1].id).isEqualTo("msg2")
    }

    @Test
    fun `getMessagesForSession should return empty list for new session`() = runTest {
        val session = repository.createSession("Empty")

        val messages = repository.getMessagesForSession(session.id).first()

        assertThat(messages).isEmpty()
    }

    // ========== deleteSession Tests ==========

    @Test
    fun `deleteSession should remove session from database`() = runTest {
        val session = repository.createSession("To Delete")

        repository.deleteSession(session.id)

        val retrieved = repository.getSessionById(session.id)
        assertThat(retrieved).isNull()
    }

    @Test
    fun `deleteSession should remove all associated messages`() = runTest {
        val session = repository.createSession("To Delete")

        repository.addMessage(session.id, ChatMessage.UserMessage(
            id = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            content = "Message"
        ))

        repository.deleteSession(session.id)

        val messages = repository.getMessagesForSession(session.id).first()
        assertThat(messages).isEmpty()
    }

    // ========== updateSessionTitle Tests ==========

    @Test
    fun `updateSessionTitle should change title`() = runTest {
        val session = repository.createSession("Original Title")

        repository.updateSessionTitle(session.id, "New Title")

        val updated = repository.getSessionById(session.id)
        assertThat(updated!!.title).isEqualTo("New Title")
    }

    @Test
    fun `updateSessionTitle should update timestamp`() = runTest {
        val session = repository.createSession("Test")
        val originalUpdatedAt = session.updatedAt

        Thread.sleep(10)
        repository.updateSessionTitle(session.id, "Updated")

        val updated = repository.getSessionById(session.id)
        assertThat(updated!!.updatedAt).isGreaterThan(originalUpdatedAt)
    }

    // ========== getAllSessions Tests ==========

    @Test
    fun `getAllSessions should return all sessions`() = runTest {
        repository.createSession("Session 1")
        repository.createSession("Session 2")
        repository.createSession("Session 3")

        val sessions = repository.getAllSessions().first()

        assertThat(sessions).hasSize(3)
    }

    @Test
    fun `getAllSessions should return empty list when no sessions`() = runTest {
        val sessions = repository.getAllSessions().first()

        assertThat(sessions).isEmpty()
    }

    // ========== getLatestSession Tests ==========

    @Test
    fun `getLatestSession should return most recent session`() = runTest {
        repository.createSession("First")
        Thread.sleep(10)
        repository.createSession("Second")
        Thread.sleep(10)
        val third = repository.createSession("Third")

        val latest = repository.getLatestSession()

        assertThat(latest).isNotNull()
        assertThat(latest!!.id).isEqualTo(third.id)
    }

    @Test
    fun `getLatestSession should return null when no sessions`() = runTest {
        val latest = repository.getLatestSession()

        assertThat(latest).isNull()
    }

    // ========== clearSessionMessages Tests ==========

    @Test
    fun `clearSessionMessages should remove all messages from session`() = runTest {
        val session = repository.createSession("Test")

        repository.addMessage(session.id, ChatMessage.UserMessage(
            id = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            content = "Message 1"
        ))
        repository.addMessage(session.id, ChatMessage.AiMessage(
            id = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            content = "Message 2"
        ))

        repository.clearSessionMessages(session.id)

        val messages = repository.getMessagesForSession(session.id).first()
        assertThat(messages).isEmpty()
    }

    @Test
    fun `clearSessionMessages should not delete session`() = runTest {
        val session = repository.createSession("Test")

        repository.clearSessionMessages(session.id)

        val retrieved = repository.getSessionById(session.id)
        assertThat(retrieved).isNotNull()
    }

    // ========== Message Type Tests ==========

    @Test
    fun `should persist and retrieve UserMessage`() = runTest {
        val session = repository.createSession("Test")
        val message = ChatMessage.UserMessage(
            id = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            content = "User message",
            attachedImageBase64 = "base64image"
        )

        repository.addMessage(session.id, message)

        val retrieved = repository.getMessagesForSession(session.id).first()[0]
        assertThat(retrieved).isInstanceOf(ChatMessage.UserMessage::class.java)
        val userMessage = retrieved as ChatMessage.UserMessage
        assertThat(userMessage.content).isEqualTo("User message")
        assertThat(userMessage.attachedImageBase64).isEqualTo("base64image")
    }

    @Test
    fun `should persist and retrieve AiMessage`() = runTest {
        val session = repository.createSession("Test")
        val message = ChatMessage.AiMessage(
            id = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            content = "AI response",
            isSuccess = true,
            errorMessage = null
        )

        repository.addMessage(session.id, message)

        val retrieved = repository.getMessagesForSession(session.id).first()[0]
        assertThat(retrieved).isInstanceOf(ChatMessage.AiMessage::class.java)
        val aiMessage = retrieved as ChatMessage.AiMessage
        assertThat(aiMessage.content).isEqualTo("AI response")
        assertThat(aiMessage.isSuccess).isTrue()
    }

    @Test
    fun `should persist and retrieve ToolCallMessage`() = runTest {
        val session = repository.createSession("Test")
        val message = ChatMessage.ToolCallMessage(
            id = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            toolName = "click",
            parameters = mapOf("x" to 540, "y" to 1200),
            result = "Success",
            isSuccess = true
        )

        repository.addMessage(session.id, message)

        val retrieved = repository.getMessagesForSession(session.id).first()[0]
        assertThat(retrieved).isInstanceOf(ChatMessage.ToolCallMessage::class.java)
        val toolMessage = retrieved as ChatMessage.ToolCallMessage
        assertThat(toolMessage.toolName).isEqualTo("click")
        assertThat(toolMessage.parameters["x"]).isEqualTo(540.0) // Gson deserializes as Double
        assertThat(toolMessage.isSuccess).isTrue()
    }

    @Test
    fun `should persist and retrieve StatusMessage`() = runTest {
        val session = repository.createSession("Test")
        val message = ChatMessage.StatusMessage(
            id = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            status = "Running...",
            isRunning = true
        )

        repository.addMessage(session.id, message)

        val retrieved = repository.getMessagesForSession(session.id).first()[0]
        assertThat(retrieved).isInstanceOf(ChatMessage.StatusMessage::class.java)
        val statusMessage = retrieved as ChatMessage.StatusMessage
        assertThat(statusMessage.status).isEqualTo("Running...")
        assertThat(statusMessage.isRunning).isTrue()
    }

    /**
     * Test implementation of ChatRepository that uses injected database
     */
    // Note: ChatRepository is now a final class with private constructor
    // Testing is done through the public API via MyApplication.getInstance().getTaskEngine()
    // This test class has been updated to test through the singleton instance
}
