package com.example.myapplication

import android.app.Application
import android.content.Context
import com.example.myapplication.api.ZhipuApiClient
import com.example.myapplication.engine.TaskEngine
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

        // API key is automatically loaded from PreferencesManager by ZhipuApiClient
        logger.i("Application initialized")
    }
}
