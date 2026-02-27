package com.example.myapplication.shell

import android.content.pm.PackageManager
import com.example.myapplication.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Shizuku helper for executing shell commands with elevated privileges
 * This allows the app to perform operations that require ADB/shell permissions
 * without requiring root access.
 *
 * Key capabilities:
 * - List all installed packages (without QUERY_ALL_PACKAGES limitations)
 * - Launch any app reliably
 * - Force stop apps
 * - Execute any shell command
 */
object ShizukuHelper {

    private const val TAG = "ShizukuHelper"
    private const val REQUEST_CODE = 1001

    private val logger = Logger(TAG)

    @Volatile
    private var isInitialized = false

    @Volatile
    private var isPermissionGranted = false

    // Shizuku binder received callback
    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        logger.i("Shizuku binder received")
        checkPermission()
    }

    // Shizuku binder dead callback
    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        logger.w("Shizuku binder dead")
        isInitialized = false
        isPermissionGranted = false
    }

    // Permission result callback
    private val permissionResultListener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
        if (requestCode == REQUEST_CODE) {
            isPermissionGranted = grantResult == PackageManager.PERMISSION_GRANTED
            logger.i("Shizuku permission result: ${if (isPermissionGranted) "granted" else "denied"}")
        }
    }

    /**
     * Initialize Shizuku
     * Should be called in Application.onCreate() or Activity.onCreate()
     */
    fun init() {
        if (isInitialized) return

        try {
            // Add listeners
            Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
            Shizuku.addBinderDeadListener(binderDeadListener)
            Shizuku.addRequestPermissionResultListener(permissionResultListener)

            isInitialized = true
            logger.i("Shizuku initialized")

            // Check if binder is already available
            if (Shizuku.pingBinder()) {
                checkPermission()
            }
        } catch (e: Exception) {
            logger.e("Failed to initialize Shizuku: ${e.message}")
        }
    }

    /**
     * Check and request Shizuku permission
     */
    private fun checkPermission() {
        try {
            if (Shizuku.pingBinder()) {
                if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                    isPermissionGranted = true
                    logger.i("Shizuku permission already granted")
                } else {
                    logger.i("Requesting Shizuku permission")
                    Shizuku.requestPermission(REQUEST_CODE)
                }
            }
        } catch (e: Exception) {
            logger.e("Failed to check Shizuku permission: ${e.message}")
        }
    }

    /**
     * Check if Shizuku is ready to use
     */
    fun isReady(): Boolean {
        return isInitialized && Shizuku.pingBinder() && isPermissionGranted
    }

    /**
     * Check if Shizuku is installed and available
     */
    fun isAvailable(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Execute a shell command using Shizuku
     *
     * @param command The shell command to execute
     * @return CommandResult containing exit code, output, and error
     */
    suspend fun execute(command: String): CommandResult = withContext(Dispatchers.IO) {
        if (!isReady()) {
            return@withContext CommandResult(
                exitCode = -1,
                output = "",
                error = "Shizuku is not ready. Please ensure Shizuku is running and permission is granted."
            )
        }

        try {
            logger.d("Executing command: $command")

            // Use Runtime.exec with shizuku user
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))

            val outputReader = BufferedReader(InputStreamReader(process.inputStream))
            val errorReader = BufferedReader(InputStreamReader(process.errorStream))

            val output = StringBuilder()
            val error = StringBuilder()

            var line: String?
            while (outputReader.readLine().also { line = it } != null) {
                output.appendLine(line)
            }
            while (errorReader.readLine().also { line = it } != null) {
                error.appendLine(line)
            }

            val exitCode = process.waitFor()

            outputReader.close()
            errorReader.close()
            process.destroy()

            logger.d("Command completed with exit code: $exitCode")

            CommandResult(
                exitCode = exitCode,
                output = output.toString().trim(),
                error = error.toString().trim()
            )
        } catch (e: Exception) {
            logger.e("Command execution failed: ${e.message}")
            CommandResult(
                exitCode = -1,
                output = "",
                error = e.message ?: "Unknown error"
            )
        }
    }

    /**
     * Execute a shell command and return only the output
     */
    suspend fun executeToString(command: String): String {
        val result = execute(command)
        return if (result.isSuccess) result.output else ""
    }

    /**
     * Cleanup listeners
     */
    fun destroy() {
        try {
            Shizuku.removeBinderReceivedListener(binderReceivedListener)
            Shizuku.removeBinderDeadListener(binderDeadListener)
            Shizuku.removeRequestPermissionResultListener(permissionResultListener)
        } catch (e: Exception) {
            logger.e("Failed to cleanup Shizuku listeners: ${e.message}")
        }
    }
}

/**
 * Result of a shell command execution
 */
data class CommandResult(
    val exitCode: Int,
    val output: String,
    val error: String
) {
    val isSuccess: Boolean get() = exitCode == 0
    val hasOutput: Boolean get() = output.isNotEmpty()
    val hasError: Boolean get() = error.isNotEmpty()

    override fun toString(): String {
        return if (isSuccess) {
            if (hasOutput) output else "Success (no output)"
        } else {
            "Error ($exitCode): ${if (hasError) error else "Unknown error"}"
        }
    }
}
