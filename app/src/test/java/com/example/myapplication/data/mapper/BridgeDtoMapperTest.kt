package com.example.myapplication.data.mapper

import com.example.myapplication.agent.LangChainAgentEngine
import com.example.myapplication.data.mapper.BridgeDtoMapper.toDto
import com.example.myapplication.data.mapper.BridgeDtoMapper.toMessageDtos
import com.example.myapplication.data.mapper.BridgeDtoMapper.toSessionDtos
import com.example.myapplication.data.model.ChatMessage
import com.example.myapplication.data.model.ChatSession
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BridgeDtoMapperTest {

    @Test
    fun session_toDto_mapsAllFields() {
        val session = ChatSession(
            id = "s1",
            title = "Hello",
            createdAt = 1000L,
            updatedAt = 2000L,
        )
        val dto = session.toDto()
        assertThat(dto.id).isEqualTo("s1")
        assertThat(dto.title).isEqualTo("Hello")
        assertThat(dto.createdAt).isEqualTo(1000L)
        assertThat(dto.updatedAt).isEqualTo(2000L)
    }

    @Test
    fun sessions_toSessionDtos_preservesOrder() {
        val list = listOf(
            ChatSession("a", "A", 1, 1),
            ChatSession("b", "B", 2, 2),
        )
        val dtos = list.toSessionDtos()
        assertThat(dtos.map { it.id }).containsExactly("a", "b").inOrder()
    }

    @Test
    fun userMessage_toDto() {
        val msg = ChatMessage.UserMessage(
            id = "m1",
            timestamp = 10L,
            content = "hi",
            attachedImageBase64 = "img",
        )
        val dto = msg.toDto()
        assertThat(dto.type).isEqualTo("user")
        assertThat(dto.content).isEqualTo("hi")
        assertThat(dto.imageBase64).isEqualTo("img")
        assertThat(dto.toolName).isNull()
    }

    @Test
    fun aiMessage_toDto() {
        val msg = ChatMessage.AiMessage(
            id = "m2",
            timestamp = 20L,
            content = "ok",
            isSuccess = false,
            errorMessage = "err",
        )
        val dto = msg.toDto()
        assertThat(dto.type).isEqualTo("ai")
        assertThat(dto.isSuccess).isFalse()
        assertThat(dto.errorMessage).isEqualTo("err")
    }

    @Test
    fun toolCall_toDto() {
        val msg = ChatMessage.ToolCallMessage(
            id = "m3",
            timestamp = 30L,
            toolName = "click",
            parameters = mapOf("x" to 1, "y" to 2),
            result = "done",
            isSuccess = true,
        )
        val dto = msg.toDto()
        assertThat(dto.type).isEqualTo("toolCall")
        assertThat(dto.toolName).isEqualTo("click")
        assertThat(dto.parameters).containsEntry("x", 1)
        assertThat(dto.result).isEqualTo("done")
        assertThat(dto.content).isEqualTo("click")
    }

    @Test
    fun screenshot_toDto() {
        val msg = ChatMessage.ScreenshotMessage(
            id = "m4",
            timestamp = 40L,
            imageBase64 = "base64",
            description = "screen",
        )
        val dto = msg.toDto()
        assertThat(dto.type).isEqualTo("screenshot")
        assertThat(dto.content).isEqualTo("screen")
        assertThat(dto.imageBase64).isEqualTo("base64")
    }

    @Test
    fun status_toDto() {
        val msg = ChatMessage.StatusMessage(
            id = "m5",
            timestamp = 50L,
            status = "running",
            isRunning = true,
        )
        val dto = msg.toDto()
        assertThat(dto.type).isEqualTo("status")
        assertThat(dto.content).isEqualTo("running")
        assertThat(dto.isRunning).isTrue()
    }

    @Test
    fun messages_toMessageDtos() {
        val msgs = listOf(
            ChatMessage.UserMessage("u", 1, "a"),
            ChatMessage.AiMessage("a", 2, "b"),
        )
        assertThat(msgs.toMessageDtos()).hasSize(2)
        assertThat(msgs.toMessageDtos().map { it.type }).containsExactly("user", "ai").inOrder()
    }

    @Test
    fun agentState_toDto_emptyStepFields() {
        val state = LangChainAgentEngine.AgentState(
            state = LangChainAgentEngine.AgentStateType.RUNNING,
            result = null,
            error = null,
        )
        val dto = state.toDto()
        assertThat(dto.state).isEqualTo("RUNNING")
        assertThat(dto.step).isEmpty()
        assertThat(dto.action).isEmpty()
        assertThat(dto.thinking).isEmpty()
        assertThat(dto.result).isNull()
        assertThat(dto.error).isNull()
    }

    @Test
    fun agentState_forceCancelled_mapsToCancelled() {
        val state = LangChainAgentEngine.AgentState(
            state = LangChainAgentEngine.AgentStateType.IDLE,
        )
        val dto = state.toDto(forceCancelled = true)
        assertThat(dto.state).isEqualTo("CANCELLED")
    }

    @Test
    fun agentState_completed_withResult() {
        val state = LangChainAgentEngine.AgentState(
            state = LangChainAgentEngine.AgentStateType.COMPLETED,
            result = "done",
        )
        val dto = state.toDto()
        assertThat(dto.state).isEqualTo("COMPLETED")
        assertThat(dto.result).isEqualTo("done")
    }
}
