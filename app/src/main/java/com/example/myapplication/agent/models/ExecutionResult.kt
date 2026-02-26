package com.example.myapplication.agent.models

/**
 * Execution result type for agent tasks
 */
sealed class ExecutionResult {

    /**
     * Task completed successfully
     * @param summary Completion summary
     */
    data class Success(val summary: String) : ExecutionResult()

    /**
     * Task failed with error
     * @param error Error message
     * @param recoverable Whether the error is recoverable (e.g., can retry)
     */
    data class Failed(val error: String, val recoverable: Boolean = false) : ExecutionResult()

    /**
     * Task exceeded maximum steps
     * @param maxSteps The maximum step limit that was reached
     */
    data class ExceededMaxSteps(val maxSteps: Int) : ExecutionResult()

    /**
     * Task was cancelled by user
     */
    object Cancelled : ExecutionResult()

    /**
     * Convert to description string
     */
    fun toDescription(): String = when (this) {
        is Success -> "Success: $summary"
        is Failed -> "Failed: $error (recoverable=$recoverable)"
        is ExceededMaxSteps -> "Exceeded max steps: $maxSteps"
        is Cancelled -> "Cancelled"
    }
}
