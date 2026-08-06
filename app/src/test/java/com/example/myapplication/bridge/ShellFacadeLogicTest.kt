package com.example.myapplication.bridge

import android.content.Context
import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * JVM tests for ShellFacade validation / error paths.
 * Does not exercise Shizuku IPC or real process exec beyond blank validation
 * and the no-service inputText path (AutoService singleton is null under JVM).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ShellFacadeLogicTest {

    private lateinit var facade: ShellFacade

    @Before
    fun setUp() {
        val ctx = mockk<Context>(relaxed = true)
        facade = ShellFacade(ctx)
    }

    @Test
    fun runCommand_blank_returnsError() = runTest {
        val r = facade.runCommand("   ")
        assertThat(r.success).isFalse()
        assertThat(r.error).contains("command is required")
        assertThat(r.output).isEmpty()
    }

    @Test
    fun runCommand_empty_returnsError() = runTest {
        val r = facade.runCommand("")
        assertThat(r.success).isFalse()
        assertThat(r.error).contains("command is required")
    }

    @Test
    fun launchApp_blank_returnsError() = runTest {
        val r = facade.launchApp("  ")
        assertThat(r.success).isFalse()
        assertThat(r.error).contains("nameOrPackage is required")
    }

    @Test
    fun inputText_withoutAccessibility_returnsError() {
        // AutoService.getInstance() is null outside a running service.
        val r = facade.inputText("hello")
        assertThat(r.success).isFalse()
        assertThat(r.error).contains("无障碍")
    }

    @Test
    fun getShizukuStatus_returnsDto() {
        val s = facade.getShizukuStatus()
        assertThat(s.status).isAnyOf("ready", "available", "unavailable")
        // ready implies available in real helper; here we only assert shape.
        if (s.ready) {
            assertThat(s.available).isTrue()
        }
    }
}
