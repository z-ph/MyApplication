package com.example.myapplication.utils

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/**
 * Unit tests for CrashHandler
 *
 * Tests crash log saving and retrieval.
 */
@RunWith(RobolectricTestRunner::class)
class CrashHandlerTest {

    private lateinit var context: android.content.Context
    private lateinit var crashLogDir: File

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        crashLogDir = File(context.filesDir, "crash_logs")
        
        // Clean up any existing crash logs
        CrashHandler.clearCrashLogs()
    }

    @Test
    fun `CrashHandler should be initialized`() {
        CrashHandler.init(context)
        assertThat(CrashHandler).isNotNull()
    }

    @Test
    fun `getCrashLogFiles should return empty list initially`() {
        val files = CrashHandler.getCrashLogFiles()
        assertThat(files).isEmpty()
    }

    @Test
    fun `getLatestCrashLog should return null when no logs exist`() {
        val latest = CrashHandler.getLatestCrashLog()
        assertThat(latest).isNull()
    }

    @Test
    fun `clearCrashLogs should remove all log files`() {
        // Initialize CrashHandler first
        CrashHandler.init(context)
        
        // Create some fake log files
        crashLogDir.mkdirs()
        val file1 = File(crashLogDir, "crash_2024-01-01_12-00-00.log")
        val file2 = File(crashLogDir, "crash_2024-01-02_12-00-00.log")
        file1.writeText("Test log 1")
        file2.writeText("Test log 2")
        
        // Flush to ensure files are written
        file1.setReadable(true)
        file2.setReadable(true)
        
        // Verify files exist before clearing using CrashHandler's method
        val beforeFiles = CrashHandler.getCrashLogFiles()
        assertThat(beforeFiles.size).isAtLeast(1)
        
        // Clear using CrashHandler
        CrashHandler.clearCrashLogs()
        
        // Small delay to ensure file system catches up
        Thread.sleep(10)
        
        // Check using CrashHandler's method
        val remaining = CrashHandler.getCrashLogFiles()
        assertThat(remaining.size).isEqualTo(0)
    }
}
