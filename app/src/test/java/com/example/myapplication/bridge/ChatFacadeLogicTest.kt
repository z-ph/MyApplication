package com.example.myapplication.bridge

import com.example.myapplication.agent.LangChainAgentEngine
import com.example.myapplication.data.model.ChatMessage
import com.example.myapplication.data.model.ChatSession
import com.example.myapplication.data.repository.ChatRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * JVM tests for ChatFacade coordination (mocked Repository + Agent).
 * Does not exercise FloatingWindowService / full execute path.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ChatFacadeLogicTest {

    private lateinit var repository: ChatRepository
    private lateinit var agent: LangChainAgentEngine
    private val agentState = MutableStateFlow(
        LangChainAgentEngine.AgentState(state = LangChainAgentEngine.AgentStateType.READY),
    )

    @Before
    fun setUp() {
        repository = mockk(relaxed = true)
        agent = mockk(relaxed = true)
        every { agent.state } returns agentState
        every { agent.clearMemory() } returns Unit
        every { agent.cancel() } answers {
            agentState.value = LangChainAgentEngine.AgentState(
                state = LangChainAgentEngine.AgentStateType.CANCELLED,
            )
        }
        every { agent.reconfigure() } returns Result.success(Unit)
        every { repository.getAllSessions() } returns flowOf(emptyList())
        every { repository.getMessagesForSession(any()) } returns flowOf(emptyList())
    }

    /** Use runTest backgroundScope so Eagerly stateIn collectors do not fail the test. */
    private fun facade(scope: CoroutineScope): ChatFacade {
        val ctx = mockk<android.content.Context>(relaxed = true)
        every { ctx.applicationContext } returns ctx
        return ChatFacade(
            appContext = ctx,
            repository = repository,
            agent = agent,
            scope = scope,
        )
    }

    @Test
    fun createSession_clearsMemory_andSelectsNew() = runTest {
        val created = ChatSession("new", "新会话", 1, 1)
        coEvery { repository.createSession(any()) } returns created

        val f = facade(backgroundScope)
        val dto = f.createSession("自定义")
        assertThat(dto.id).isEqualTo("new")
        assertThat(dto.title).isEqualTo("新会话")
        assertThat(f.currentSessionId.value).isEqualTo("new")
        verify { agent.clearMemory() }
        coVerify { repository.createSession("自定义") }
    }

    @Test
    fun createSession_blankTitle_usesDefault() = runTest {
        coEvery { repository.createSession(any()) } answers {
            ChatSession("id", firstArg(), 1, 1)
        }
        val f = facade(backgroundScope)
        f.createSession("  ")
        coVerify { repository.createSession("新会话") }
    }

    @Test
    fun selectSession_clearsMemory_andUpdatesId() = runTest {
        val f = facade(backgroundScope)
        f.selectSession("sid-9")
        assertThat(f.currentSessionId.value).isEqualTo("sid-9")
        verify { agent.clearMemory() }
        assertThat(f.lastCancelRequested).isFalse()
    }

    @Test
    fun sendMessage_empty_throws() = runTest {
        val f = facade(backgroundScope)
        f.selectSession("s")
        try {
            f.sendMessage("   ")
            throw AssertionError("expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertThat(e.message).contains("content")
        }
    }

    @Test
    fun cancelTask_setsLastCancelRequested_andCallsAgentCancel() =
        runTest(UnconfinedTestDispatcher()) {
            coEvery { repository.addMessage(any(), any()) } returns Unit

            val f = facade(backgroundScope)
            f.selectSession("s1")
            f.cancelTask()
            assertThat(f.lastCancelRequested).isTrue()
            verify { agent.cancel() }
            assertThat(agentState.value.state)
                .isEqualTo(LangChainAgentEngine.AgentStateType.CANCELLED)
            advanceUntilIdle()
            // Single cancel UX: StatusMessage only (no AiMessage from CANCELLED branch)
            coVerify(exactly = 1) {
                repository.addMessage(
                    "s1",
                    match { it is ChatMessage.StatusMessage },
                )
            }
            coVerify(exactly = 0) {
                repository.addMessage(
                    "s1",
                    match { it is ChatMessage.AiMessage },
                )
            }
        }

    @Test
    fun listSessions_awaitsColdStartInit_selectsLatest() =
        runTest(UnconfinedTestDispatcher()) {
            val latest = ChatSession("s-latest", "最近", 1, 1)
            coEvery { repository.getLatestSession() } returns latest
            every { repository.getAllSessions() } returns flowOf(listOf(latest))

            val f = facade(backgroundScope)
            f.ensureInitialized()
            val list = f.listSessions()
            assertThat(list).hasSize(1)
            assertThat(list[0].id).isEqualTo("s-latest")
            assertThat(f.currentSessionId.value).isEqualTo("s-latest")
        }

    @Test
    fun listSessions_awaitsColdStartInit_createsWhenEmpty() =
        runTest(UnconfinedTestDispatcher()) {
            coEvery { repository.getLatestSession() } returns null
            val created = ChatSession("s-new", "新会话", 1, 1)
            coEvery { repository.createSession(any()) } returns created
            every { repository.getAllSessions() } returns flowOf(listOf(created))

            val f = facade(backgroundScope)
            f.ensureInitialized()
            val list = f.listSessions()
            assertThat(list).hasSize(1)
            assertThat(f.currentSessionId.value).isEqualTo("s-new")
            coVerify { repository.createSession("新会话") }
        }

    @Test
    fun deleteSession_current_switchesToLatestOrCreates() = runTest {
        coEvery { repository.deleteSession("cur") } returns Unit
        coEvery { repository.getLatestSession() } returns ChatSession("other", "o", 1, 1)

        val f = facade(backgroundScope)
        f.selectSession("cur")
        f.deleteSession("cur")
        assertThat(f.currentSessionId.value).isEqualTo("other")
        coVerify { repository.deleteSession("cur") }
    }

    @Test
    fun listMessages_mapsViaRepository() = runTest {
        coEvery { repository.getMessagesForSessionSync("s1") } returns listOf(
            ChatMessage.UserMessage("u", 1, "hello"),
        )
        val f = facade(backgroundScope)
        val msgs = f.listMessages("s1")
        assertThat(msgs).hasSize(1)
        assertThat(msgs[0].type).isEqualTo("user")
        assertThat(msgs[0].content).isEqualTo("hello")
    }

    @Test
    fun agentFacade_getState_whenCancelled() = runTest {
        agentState.value = LangChainAgentEngine.AgentState(
            state = LangChainAgentEngine.AgentStateType.CANCELLED,
        )
        val chat = facade(backgroundScope)
        val agentFacade = AgentFacade(agent, chat)
        assertThat(agentFacade.getState().state).isEqualTo("CANCELLED")
    }

    @Test
    fun agentFacade_getState_forceCancelledWhenIdleAfterCancelFlag() = runTest {
        val chat = facade(backgroundScope)
        chat.selectSession("s")
        chat.cancelTask()
        // Simulate legacy engine writing IDLE after cancel
        agentState.value = LangChainAgentEngine.AgentState(
            state = LangChainAgentEngine.AgentStateType.IDLE,
        )
        val agentFacade = AgentFacade(agent, chat)
        assertThat(agentFacade.getState().state).isEqualTo("CANCELLED")
    }

    @Test
    fun clearMessages_usesCurrentSession() = runTest {
        coEvery { repository.clearSessionMessages("s1") } returns Unit
        val f = facade(backgroundScope)
        f.selectSession("s1")
        f.clearMessages()
        coVerify { repository.clearSessionMessages("s1") }
    }
}
