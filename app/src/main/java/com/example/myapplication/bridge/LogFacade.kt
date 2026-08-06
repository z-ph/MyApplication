package com.example.myapplication.bridge

import com.example.myapplication.utils.LogEntry
import com.example.myapplication.utils.LogLevel
import com.example.myapplication.utils.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * UI-free log façade over [Logger] in-memory buffer.
 * Contract: docs/bridge-api.md §3.5 LingxiLog
 */
class LogFacade {

    data class LogEntryDto(
        val id: String,
        val timestamp: String,
        val tag: String,
        val level: String,
        val message: String,
        val throwable: String? = null,
    )

    fun list(): List<LogEntryDto> = Logger.logEntries.value.map { it.toDto() }

    fun logsFlow(): Flow<List<LogEntryDto>> =
        Logger.logEntries.map { entries -> entries.map { it.toDto() } }

    fun clear() {
        Logger.clearLogs()
    }

    /** Plain-text export for clipboard / share. */
    fun exportAsText(): String = Logger.exportLogsAsString()

    private fun LogEntry.toDto(): LogEntryDto = LogEntryDto(
        id = id,
        timestamp = timestamp,
        tag = tag,
        level = when (level) {
            LogLevel.ERROR -> "ERROR"
            LogLevel.WARN -> "WARN"
            LogLevel.INFO -> "INFO"
            LogLevel.DEBUG -> "DEBUG"
            LogLevel.VERBOSE -> "VERBOSE"
        },
        message = message,
        throwable = throwable,
    )
}
