package com.example.myapplication.utils

import android.content.Context
import android.os.Build
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.*

/**
 * Global Crash Handler
 * Captures uncaught exceptions and saves crash logs
 */
object CrashHandler : Thread.UncaughtExceptionHandler {

    private const val TAG = "CrashHandler"
    private const val CRASH_LOG_DIR = "crash_logs"
    private const val MAX_LOG_FILES = 10
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US)
    
    private val logger = Logger(TAG)
    private var originalHandler: Thread.UncaughtExceptionHandler? = null
    private var context: Context? = null
    private var crashLogDir: File? = null

    /**
     * Initialize crash handler
     * Must be called in Application.onCreate()
     */
    fun init(context: Context) {
        this.context = context.applicationContext
        crashLogDir = File(context.filesDir, CRASH_LOG_DIR)
        
        if (!crashLogDir!!.exists()) {
            crashLogDir!!.mkdirs()
        }
        
        originalHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(this)
        
        logger.i("CrashHandler initialized")
    }

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        logger.e("Uncaught exception in thread '${thread.name}': ${throwable.message}", throwable)
        
        try {
            // Save crash log to file
            val crashLog = buildCrashLog(thread, throwable)
            saveCrashLog(crashLog)
            
            // Clean up old crash logs
            cleanupOldCrashLogs()
        } catch (e: Exception) {
            logger.e("Failed to save crash log: ${e.message}")
        }
        
        // Delegate to original handler (may show system crash dialog)
        originalHandler?.uncaughtException(thread, throwable)
    }

    private fun buildCrashLog(thread: Thread, throwable: Throwable): String {
        val timestamp = dateFormat.format(Date())
        val deviceInfo = buildDeviceInfo()
        val stackTrace = getStackTrace(throwable)
        
        return buildString {
            appendLine("=".repeat(60))
            appendLine("CRASH REPORT")
            appendLine("=".repeat(60))
            appendLine("Timestamp: $timestamp")
            appendLine("Thread: ${thread.name} (ID: ${thread.id})")
            appendLine()
            appendLine(deviceInfo)
            appendLine()
            appendLine("=".repeat(60))
            appendLine("EXCEPTION DETAILS")
            appendLine("=".repeat(60))
            appendLine("Type: ${throwable.javaClass.name}")
            appendLine("Message: ${throwable.message}")
            appendLine()
            appendLine("Stack Trace:")
            appendLine(stackTrace)
            
            // Include cause if present
            var cause = throwable.cause
            while (cause != null) {
                appendLine()
                appendLine("Caused by: ${cause.javaClass.name}: ${cause.message}")
                appendLine(getStackTrace(cause))
                cause = cause.cause
            }
            
            appendLine("=".repeat(60))
        }
    }

    private fun buildDeviceInfo(): String {
        return buildString {
            appendLine("Device Information:")
            appendLine("  Brand: ${Build.BRAND}")
            appendLine("  Model: ${Build.MODEL}")
            appendLine("  Manufacturer: ${Build.MANUFACTURER}")
            appendLine("  Device: ${Build.DEVICE}")
            appendLine("  Product: ${Build.PRODUCT}")
            appendLine("  Android Version: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("  Security Patch: ${Build.VERSION.SECURITY_PATCH}")
            appendLine("  SDK: ${Build.VERSION.SDK}")
            appendLine("  Fingerprint: ${Build.FINGERPRINT}")
            appendLine()
            appendLine("App Information:")
            appendLine("  Version Code: ${context?.packageManager?.getPackageInfo(context?.packageName ?: "", 0)?.versionCode}")
            appendLine("  Version Name: ${context?.packageManager?.getPackageInfo(context?.packageName ?: "", 0)?.versionName}")
        }
    }

    private fun getStackTrace(throwable: Throwable): String {
        val writer = StringWriter()
        val printer = PrintWriter(writer)
        throwable.printStackTrace(printer)
        return writer.toString()
    }

    private fun saveCrashLog(crashLog: String) {
        val crashLogDir = crashLogDir ?: return
        
        val timestamp = dateFormat.format(Date())
        val fileName = "crash_$timestamp.log"
        val file = File(crashLogDir, fileName)
        
        file.writeText(crashLog)
        logger.e("Crash log saved to: ${file.absolutePath}")
    }

    private fun cleanupOldCrashLogs() {
        val crashLogDir = crashLogDir ?: return
        
        val logFiles = crashLogDir.listFiles { file ->
            file.name.endsWith(".log")
        }?.sortedByDescending { it.lastModified() } ?: return
        
        if (logFiles.size > MAX_LOG_FILES) {
            for (i in MAX_LOG_FILES until logFiles.size) {
                logFiles[i].delete()
            }
        }
    }

    /**
     * Get list of crash log files
     */
    fun getCrashLogFiles(): List<File> {
        return crashLogDir?.listFiles { file ->
            file.name.endsWith(".log")
        }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    /**
     * Get latest crash log content
     */
    fun getLatestCrashLog(): String? {
        val files = getCrashLogFiles()
        return files.firstOrNull()?.readText()
    }

    /**
     * Clear all crash logs
     */
    fun clearCrashLogs() {
        crashLogDir?.listFiles()?.forEach { it.delete() }
    }
}
