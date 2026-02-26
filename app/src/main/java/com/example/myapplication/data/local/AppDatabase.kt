package com.example.myapplication.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.myapplication.data.local.converters.Converters
import com.example.myapplication.data.local.dao.ApiConfigDao
import com.example.myapplication.data.local.dao.MessageDao
import com.example.myapplication.data.local.dao.SessionDao
import com.example.myapplication.data.local.entities.ApiConfigEntity
import com.example.myapplication.data.local.entities.MessageEntity
import com.example.myapplication.data.local.entities.SessionEntity

/**
 * Room database for chat data
 */
@Database(
    entities = [
        SessionEntity::class,
        MessageEntity::class,
        ApiConfigEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun messageDao(): MessageDao
    abstract fun apiConfigDao(): ApiConfigDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "chat_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        /**
         * Migration from version 1 to 2: Add api_configs table
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS api_configs (
                        id TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        providerId TEXT NOT NULL,
                        apiKey TEXT NOT NULL,
                        baseUrl TEXT NOT NULL,
                        modelId TEXT NOT NULL,
                        isActive INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }
    }
}
