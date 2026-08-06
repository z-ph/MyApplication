package com.example.myapplication.bridge

import com.example.myapplication.agent.LangChainAgentEngine
import com.example.myapplication.data.dto.AgentStateDto
import com.example.myapplication.data.mapper.BridgeDtoMapper.toDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Thin façade for LingxiAgent plugin (docs/bridge-api.md §3.2).
 * getState / stateChanged return bare AgentStateDto (no wrapper).
 */
class AgentFacade(
    private val agent: LangChainAgentEngine,
    private val chatFacade: ChatFacade? = null,
) {
    fun getState(): AgentStateDto {
        val forceCancelled = chatFacade?.lastCancelRequested == true &&
            agent.state.value.state == LangChainAgentEngine.AgentStateType.IDLE
        return agent.state.value.toDto(forceCancelled = forceCancelled)
    }

    fun stateFlow(): Flow<AgentStateDto> {
        return agent.state.map { state ->
            val forceCancelled = chatFacade?.lastCancelRequested == true &&
                state.state == LangChainAgentEngine.AgentStateType.IDLE
            // If engine already has CANCELLED, map as-is.
            if (state.state == LangChainAgentEngine.AgentStateType.CANCELLED) {
                state.toDto(forceCancelled = false)
            } else {
                state.toDto(forceCancelled = forceCancelled)
            }
        }
    }

    fun reconfigure(): Result<Unit> = agent.reconfigure()

    suspend fun isConfigured(): Boolean = agent.isConfigured()
}
