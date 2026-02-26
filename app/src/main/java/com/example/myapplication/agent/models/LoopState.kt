package com.example.myapplication.agent.models

/**
 * Agent Loop State - State machine pattern for agent execution
 *
 * State flow:
 * Idle -> Thinking -> Acting -> Observing -> Thinking -> ... -> Completed/Failed
 *
 * In Acting state, multiple tools can be executed sequentially:
 * Acting(pendingTools=[A,B,C]) -> Acting(pendingTools=[B,C]) -> ... -> Observing
 */
sealed class AgentLoopState {

    /**
     * Initial idle state, waiting for task
     */
    object Idle : AgentLoopState()

    /**
     * AI is thinking/processing
     * @param context Current loop context including step count and history
     */
    data class Thinking(val context: LoopContext) : AgentLoopState()

    /**
     * Executing tools
     * Supports multi-tool sequential execution
     * @param pendingTools Tools waiting to be executed
     * @param executedResults Results from already executed tools
     * @param context Current loop context
     */
    data class Acting(
        val pendingTools: List<ToolCallInfo>,
        val executedResults: List<ToolResult>,
        val context: LoopContext
    ) : AgentLoopState()

    /**
     * Observing results after tool execution
     * @param result Combined result from all tool executions
     * @param context Current loop context
     */
    data class Observing(
        val result: ToolResult,
        val context: LoopContext
    ) : AgentLoopState()

    /**
     * Task completed successfully
     * @param summary Task completion summary
     */
    data class Completed(val summary: String) : AgentLoopState()

    /**
     * Task failed
     * @param error Error message
     * @param recoverable Whether the error is recoverable
     */
    data class Failed(val error: String, val recoverable: Boolean = false) : AgentLoopState()
}

/**
 * Loop execution context
 */
data class LoopContext(
    val currentStep: Int = 0,
    val maxSteps: Int = 20,
    val goal: String = "",
    val thinkingContent: String? = null
)

/**
 * Tool call information
 */
data class ToolCallInfo(
    val id: String = "",
    val name: String,
    val parameters: Map<String, Any>
)

/**
 * Tool execution result
 */
data class ToolResult(
    val success: Boolean,
    val output: String,
    val error: String? = null
) {
    companion object {
        fun success(output: String) = ToolResult(true, output)
        fun failure(error: String) = ToolResult(false, error, error)
    }
}
