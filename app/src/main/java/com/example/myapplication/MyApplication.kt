package com.example.myapplication

import android.app.Application
import android.content.Context
import com.example.myapplication.api.ZhipuApiClient
import com.example.myapplication.engine.TaskEngine
import com.example.myapplication.shell.ShizukuHelper
import com.example.myapplication.shell.ShellExecutor
import com.example.myapplication.utils.Logger

/**
 * Application class for dependency injection and app-level initialization
 */
class MyApplication : Application() {

    companion object {
        private const val TAG = "MyApplication"

        @Volatile
        private var instance: MyApplication? = null

        fun getInstance(): MyApplication {
            return instance ?: synchronized(this) {
                instance ?: throw IllegalStateException("Application not initialized")
            }
        }

        fun getAppContext(): Context {
            return getInstance().applicationContext
        }

        // Dependency accessors
        fun getTaskEngine(): TaskEngine = getInstance().taskEngine
        fun getZhipuApiClient(): ZhipuApiClient = getInstance().zhipuApiClient
        fun getApiClient(): ZhipuApiClient = getInstance().zhipuApiClient  // Alias for convenience
        fun getShellExecutor(): ShellExecutor = getInstance().shellExecutor
    }

    private val logger = Logger(TAG)

    // Singletons
    val zhipuApiClient: ZhipuApiClient by lazy {
        logger.d("Creating ZhipuApiClient")
        ZhipuApiClient(applicationContext)
    }

    val taskEngine: TaskEngine by lazy {
        logger.d("Creating TaskEngine")
        TaskEngine(applicationContext).apply {
            apiClient = zhipuApiClient
        }
    }

    val shellExecutor: ShellExecutor by lazy {
        logger.d("Creating ShellExecutor")
        ShellExecutor(applicationContext)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        logger.i("MyApplication created")

        // Initialize app components
        initializeApp()
    }

    private fun initializeApp() {
        // Initialize logger with file logging if needed
        val filesDir = applicationContext.getExternalFilesDir(null)
        if (filesDir != null) {
            Logger.enableFileLogging(filesDir)
            logger.d("File logging enabled to ${filesDir.absolutePath}")
        }

        // Initialize Shizuku for shell access
        try {
            ShizukuHelper.init()
            logger.i("Shizuku initialized")
        } catch (e: Exception) {
            logger.w("Shizuku not available: ${e.message}")
        }

        // API key is automatically loaded from PreferencesManager by ZhipuApiClient
        logger.i("Application initialized")
    }
}
