package com.example.myapplication.data.local.dao

import androidx.room.*
import com.example.myapplication.data.local.entities.ApiConfigEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for API configuration operations
 */
@Dao
interface ApiConfigDao {

    @Query("SELECT * FROM api_configs ORDER BY updatedAt DESC")
    fun getAllConfigs(): Flow<List<ApiConfigEntity>>

    @Query("SELECT * FROM api_configs WHERE id = :configId")
    suspend fun getConfigById(configId: String): ApiConfigEntity?

    @Query("SELECT * FROM api_configs WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveConfig(): ApiConfigEntity?

    @Query("SELECT * FROM api_configs WHERE isActive = 1 LIMIT 1")
    fun getActiveConfigFlow(): Flow<ApiConfigEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfig(config: ApiConfigEntity)

    @Update
    suspend fun updateConfig(config: ApiConfigEntity)

    @Delete
    suspend fun deleteConfig(config: ApiConfigEntity)

    @Query("DELETE FROM api_configs WHERE id = :configId")
    suspend fun deleteConfigById(configId: String)

    @Query("UPDATE api_configs SET isActive = 0")
    suspend fun clearActiveConfig()

    @Transaction
    suspend fun setActiveConfig(configId: String) {
        clearActiveConfig()
        getConfigById(configId)?.let { config ->
            updateConfig(config.copy(isActive = true, updatedAt = System.currentTimeMillis()))
        }
    }

    @Query("SELECT COUNT(*) FROM api_configs")
    suspend fun getConfigCount(): Int

    @Query("SELECT * FROM api_configs WHERE providerId = :providerId LIMIT 1")
    suspend fun getConfigByProvider(providerId: String): ApiConfigEntity?
}
