package com.example.myapplication.bridge

import android.content.Context
import com.example.myapplication.agent.LangChainAgentEngine
import com.example.myapplication.data.dto.ChatMessageDto
import com.example.myapplication.data.dto.ChatSessionDto
import com.example.myapplication.data.mapper.BridgeDtoMapper.toDto
import com.example.myapplication.data.mapper.BridgeDtoMapper.toMessageDtos
import com.example.myapplication.data.mapper.BridgeDtoMapper.toSessionDtos
import com.example.myapplication.data.model.ChatMessage
import com.example.myapplication.data.repository.ChatRepository
import com.example.myapplication.ui.overlay.FloatingWindowService
import com.example.myapplication.utils.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * UI-free chat façade for LingxiChatPlugin.
 * Owns FloatingWindowService.onStopButtonClick (single owner after Compose removal).
 *
 * Contract: docs/bridge-api.md §3.1 LingxiChat
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ChatFacade(
    private val appContext: Context,
    private val repository: ChatRepository,
    private val agent: LangChainAgentEngine,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
) {
    private val logger = Logger(TAG)

    private val _currentSessionId = MutableStateFlow<String?>(null)
    val currentSessionId: StateFlow<String?> = _currentSessionId.asStateFlow()

    val sessions: StateFlow<List<ChatSessionDto>> = repository.getAllSessions()
        .map { it.toSessionDtos() }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    val currentMessages: StateFlow<List<ChatMessageDto>> = _currentSessionId
        .filterNotNull()
        .flatMapLatest { sessionId ->
            repository.getMessagesForSession(sessionId).map { msgs ->
                msgs.toMessageDtos()
            }
        }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    private var currentTaskJob: Job? = null
    private var floatingWindowJob: Job? = null
    private var initJob: Job? = null
    private var initialized = false

    /** Set when user cancels so H5 sees CANCELLED even if engine wrote IDLE. */
    @Volatile
    var lastCancelRequested: Boolean = false
        private set

    fun ensureInitialized() {
        if (initialized) return
        initialized = true
        initJob = scope.launch {
            val latest = repository.getLatestSession()
            if (latest != null) {
                _currentSessionId.value = latest.id
            } else {
                createSession("新会话")
            }
        }
        setupFloatingWindowSync()
    }

    /** Wait for cold-start session create/select so listSessions is not empty. */
    private suspend fun awaitInitialized() {
        initJob?.join()
    }

    private fun setupFloatingWindowSync() {
        floatingWindowJob?.cancel()
        floatingWindowJob = scope.launch {
            agent.state.collect { state ->
                FloatingWindowService.getInstance()?.setLangChainAgentState(state)
            }
        }
        FloatingWindowService.onStopButtonClick = {
            cancelTask()
        }
    }

    suspend fun listSessions(): List<ChatSessionDto> {
        awaitInitialized()
        return repository.getAllSessions().first().toSessionDtos()
    }

    suspend fun createSession(title: String? = null): ChatSessionDto {
        agent.clearMemory()
        lastCancelRequested = false
        val session = repository.createSession(title?.takeIf { it.isNotBlank() } ?: "新会话")
        _currentSessionId.value = session.id
        return session.toDto()
    }

    fun selectSession(sessionId: String) {
        agent.clearMemory()
        lastCancelRequested = false
        _currentSessionId.value = sessionId
    }

    suspend fun deleteSession(sessionId: String) {
        repository.deleteSession(sessionId)
        if (_currentSessionId.value == sessionId) {
            agent.clearMemory()
            val latest = repository.getLatestSession()
            if (latest != null) {
                _currentSessionId.value = latest.id
            } else {
                createSession("新会话")
            }
        }
    }

    suspend fun listMessages(sessionId: String): List<ChatMessageDto> {
        return repository.getMessagesForSessionSync(sessionId).toMessageDtos()
    }

    /**
     * Send user message and start agent task (async). Returns after user message is persisted.
     */
    suspend fun sendMessage(content: String) {
        val trimmed = content.trim()
        require(trimmed.isNotEmpty()) { "content is required" }

        var sessionId = _currentSessionId.value
        if (sessionId == null) {
            sessionId = createSession("新会话").id
        }

        lastCancelRequested = false

        val userMessage = ChatMessage.UserMessage(
            id = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            content = trimmed,
            attachedImageBase64 = null,
        )
        repository.addMessage(sessionId, userMessage)

        val existing = repository.getMessagesForSessionSync(sessionId)
        if (existing.size <= 1) {
            val title = trimmed.take(30).let { if (trimmed.length > 30) "$it..." else it }
            repository.updateSessionTitle(sessionId, title)
        }

        executeWithAgent(sessionId, trimmed)
    }

    private fun executeWithAgent(sessionId: String, instruction: String) {
        currentTaskJob?.cancel()
        currentTaskJob = scope.launch {
            FloatingWindowService.start(appContext)
            FloatingWindowService.getInstance()?.clearLog()
            FloatingWindowService.getInstance()?.show()

            try {
                ensureAgentReady()

                withContext(Dispatchers.IO) {
                    agent.execute(instruction) { result ->
                        scope.launch {
                            val message = if (result.success) {
                                ChatMessage.AiMessage(
                                    id = UUID.randomUUID().toString(),
                                    timestamp = System.currentTimeMillis(),
                                    content = result.message,
                                    isSuccess = true,
                                )
                            } else {
                                ChatMessage.AiMessage(
                                    id = UUID.randomUUID().toString(),
                                    timestamp = System.currentTimeMillis(),
                                    content = "❌ ${result.message}",
                                    isSuccess = false,
                                    errorMessage = result.message,
                                )
                            }
                            repository.addMessage(sessionId, message)
                        }
                    }
                }

                agent.state.filter {
                    it.state != LangChainAgentEngine.AgentStateType.RUNNING
                }.first()

                // Skip CANCELLED here: cancelTask owns the single StatusMessage cancel UX.
                val finalState = agent.state.value
                when (finalState.state) {
                    LangChainAgentEngine.AgentStateType.COMPLETED -> {
                        repository.addMessage(
                            sessionId,
                            ChatMessage.AiMessage(
                                id = UUID.randomUUID().toString(),
                                timestamp = System.currentTimeMillis(),
                                content = "✅ ${finalState.result ?: "完成"}",
                                isSuccess = true,
                            ),
                        )
                    }
                    LangChainAgentEngine.AgentStateType.ERROR -> {
                        repository.addMessage(
                            sessionId,
                            ChatMessage.AiMessage(
                                id = UUID.randomUUID().toString(),
                                timestamp = System.currentTimeMillis(),
                                content = "❌ ${finalState.error ?: "未知错误"}",
                                isSuccess = false,
                                errorMessage = finalState.error,
                            ),
                        )
                    }
                    else -> {}
                }

                kotlinx.coroutines.delay(2000)
                FloatingWindowService.getInstance()?.hide()
            } catch (e: kotlinx.coroutines.CancellationException) {
                // Mirror success/error: always hide overlay on cancel path
                FloatingWindowService.getInstance()?.hide()
                throw e
            } catch (e: Exception) {
                logger.e("Execution error: ${e.message}", e)
                repository.addMessage(
                    sessionId,
                    ChatMessage.AiMessage(
                        id = UUID.randomUUID().toString(),
                        timestamp = System.currentTimeMillis(),
                        content = "❌ 执行失败：${e.message}",
                        isSuccess = false,
                        errorMessage = e.message,
                    ),
                )
                FloatingWindowService.getInstance()?.hide()
            }
        }
    }

    /**
     * Ensure agent is READY before execute (COMPLETED/ERROR/IDLE/CANCELLED → reconfigure).
     */
    private fun ensureAgentReady() {
        val s = agent.state.value.state
        if (s != LangChainAgentEngine.AgentStateType.READY &&
            s != LangChainAgentEngine.AgentStateType.RUNNING
        ) {
            agent.reconfigure()
        }
    }

    fun cancelTask() {
        logger.d("cancelTask called")
        lastCancelRequested = true
        agent.cancel()
        currentTaskJob?.cancel()
        currentTaskJob = null
        // Hide overlay immediately (do not wait for job CancellationException)
        FloatingWindowService.getInstance()?.hide()

        // Single cancel UX message (StatusMessage only; executeWithAgent skips CANCELLED AiMessage)
        val sessionId = _currentSessionId.value ?: return
        scope.launch {
            repository.addMessage(
                sessionId,
                ChatMessage.StatusMessage(
                    id = UUID.randomUUID().toString(),
                    timestamp = System.currentTimeMillis(),
                    status = "⏹️ 任务已取消",
                    isRunning = false,
                ),
            )
        }
    }

    suspend fun clearMessages() {
        val sessionId = _currentSessionId.value ?: return
        repository.clearSessionMessages(sessionId)
    }

    fun dispose() {
        currentTaskJob?.cancel()
        floatingWindowJob?.cancel()
        agent.cancel()
        FloatingWindowService.stop(appContext)
        initialized = false
    }

    companion object {
        private const val TAG = "ChatFacade"
    }
}
