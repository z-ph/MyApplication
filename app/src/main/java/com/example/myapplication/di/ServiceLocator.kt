package com.example.myapplication.di

import android.content.Context
import com.example.myapplication.MyApplication
import com.example.myapplication.agent.LangChainAgentEngine
import com.example.myapplication.api.ModelFetcher
import com.example.myapplication.bridge.AgentFacade
import com.example.myapplication.bridge.ApiConfigFacade
import com.example.myapplication.bridge.ChatFacade
import com.example.myapplication.bridge.PermissionFacade
import com.example.myapplication.data.local.AppDatabase
import com.example.myapplication.data.local.preferences.AppPreferences
import com.example.myapplication.data.repository.ApiConfigRepository
import com.example.myapplication.data.repository.ChatRepository
import com.example.myapplication.network.LangChainHttpClientRegistry

/**
 * Simple Service Locator for dependency management
 * Provides singleton instances of repositories and services
 *
 * Note: Consider migrating to Hilt/Koin for more complex dependency injection needs
 */
object ServiceLocator {

    @Volatile
    private var database: AppDatabase? = null

    @Volatile
    private var chatRepository: ChatRepository? = null

    @Volatile
    private var apiConfigRepository: ApiConfigRepository? = null

    @Volatile
    private var appPreferences: AppPreferences? = null

    @Volatile
    private var modelFetcher: ModelFetcher? = null

    @Volatile
    private var chatFacade: ChatFacade? = null

    @Volatile
    private var agentFacade: AgentFacade? = null

    @Volatile
    private var apiConfigFacade: ApiConfigFacade? = null

    @Volatile
    private var permissionFacade: PermissionFacade? = null

    /**
     * Initialize the service locator with application context
     * Should be called in Application.onCreate()
     */
    fun init(context: Context) {
        if (database == null) {
            synchronized(this) {
                if (database == null) {
                    database = AppDatabase.getDatabase(context.applicationContext)
                }
            }
        }
    }

    /**
     * Get AppDatabase instance
     */
    fun getDatabase(): AppDatabase {
        return database ?: throw IllegalStateException("ServiceLocator not initialized. Call init() first.")
    }

    /**
     * Get ChatRepository instance (singleton)
     */
    fun getChatRepository(): ChatRepository {
        return chatRepository ?: synchronized(this) {
            chatRepository ?: ChatRepository.getInstance(getDatabase()).also { chatRepository = it }
        }
    }

    /**
     * Get ApiConfigRepository instance (singleton)
     */
    fun getApiConfigRepository(): ApiConfigRepository {
        return apiConfigRepository ?: synchronized(this) {
            apiConfigRepository ?: ApiConfigRepository(getDatabase().apiConfigDao()).also { apiConfigRepository = it }
        }
    }

    /**
     * Get AppPreferences instance (singleton)
     */
    fun getAppPreferences(context: Context): AppPreferences {
        return appPreferences ?: synchronized(this) {
            appPreferences ?: AppPreferences.getInstance(context).also { appPreferences = it }
        }
    }

    /**
     * Get ModelFetcher instance (singleton)
     */
    fun getModelFetcher(): ModelFetcher {
        return modelFetcher ?: synchronized(this) {
            modelFetcher ?: ModelFetcher().also { modelFetcher = it }
        }
    }

    private fun getAgentEngine(context: Context): LangChainAgentEngine {
        return try {
            MyApplication.getLangChainAgentEngine()
        } catch (_: Exception) {
            LangChainAgentEngine.getInstance(context.applicationContext)
        }
    }

    /**
     * Chat façade singleton for Capacitor plugins.
     */
    fun getChatFacade(context: Context): ChatFacade {
        chatFacade?.let { return it }
        synchronized(this) {
            chatFacade?.let { return it }
            init(context)
            val appCtx = context.applicationContext
            return ChatFacade(
                appContext = appCtx,
                repository = getChatRepository(),
                agent = getAgentEngine(appCtx),
            ).also { chatFacade = it }
        }
    }

    /**
     * Agent façade singleton (shares ChatFacade cancel flag for CANCELLED mapping).
     */
    fun getAgentFacade(context: Context): AgentFacade {
        agentFacade?.let { return it }
        synchronized(this) {
            agentFacade?.let { return it }
            val chat = getChatFacade(context)
            val appCtx = context.applicationContext
            return AgentFacade(
                agent = getAgentEngine(appCtx),
                chatFacade = chat,
            ).also { agentFacade = it }
        }
    }

    /**
     * API config façade singleton (CRUD + reconfigure).
     */
    fun getApiConfigFacade(context: Context): ApiConfigFacade {
        apiConfigFacade?.let { return it }
        synchronized(this) {
            apiConfigFacade?.let { return it }
            init(context)
            val appCtx = context.applicationContext
            return ApiConfigFacade(
                repository = getApiConfigRepository(),
                modelFetcher = getModelFetcher(),
                agent = getAgentEngine(appCtx),
            ).also { apiConfigFacade = it }
        }
    }

    /**
     * Permission façade singleton.
     */
    fun getPermissionFacade(context: Context): PermissionFacade {
        permissionFacade?.let { return it }
        synchronized(this) {
            permissionFacade?.let { return it }
            init(context)
            val appCtx = context.applicationContext
            return PermissionFacade(
                appContext = appCtx,
                apiConfigRepository = getApiConfigRepository(),
            ).also { permissionFacade = it }
        }
    }

    /**
     * Reset all instances (for testing purposes only)
     */
    fun reset() {
        synchronized(this) {
            LangChainHttpClientRegistry.reset()
            chatFacade?.dispose()
            chatFacade = null
            agentFacade = null
            apiConfigFacade = null
            permissionFacade = null
            database = null
            chatRepository = null
            apiConfigRepository = null
            appPreferences = null
            modelFetcher = null
        }
    }
}
