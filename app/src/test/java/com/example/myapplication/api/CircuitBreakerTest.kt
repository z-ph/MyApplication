package com.example.myapplication.api

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.LooperMode

/**
 * Unit tests for CircuitBreaker
 *
 * Tests circuit breaker state transitions and failure handling.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.PAUSED)
class CircuitBreakerTest {

    private lateinit var circuitBreaker: CircuitBreaker

    @Before
    fun setup() {
        circuitBreaker = CircuitBreaker(
            failureThreshold = 3,
            successThreshold = 2,
            resetTimeoutMs = 100
        )
    }

    // ========== Initial State Tests ==========

    @Test
    fun `initial state should be CLOSED`() {
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED)
    }

    @Test
    fun `initial failure count should be zero`() {
        assertThat(circuitBreaker.getFailureCount()).isEqualTo(0)
    }

    // ========== execute Success Tests ==========

    @Test
    fun `execute should return result on success`() = runTest {
        val result = circuitBreaker.execute { "Success" }
        assertThat(result).isEqualTo("Success")
    }

    @Test
    fun `execute should keep circuit CLOSED on success`() = runTest {
        circuitBreaker.execute { "Result" }
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED)
    }

    @Test
    fun `execute should reset failure count on success`() = runTest {
        // Simulate some failures first (but not enough to trip)
        repeat(2) {
            circuitBreaker.execute<Any> { throw RuntimeException("Error") }
        }
        
        // Success should reset
        circuitBreaker.execute { "Success" }
        assertThat(circuitBreaker.getFailureCount()).isEqualTo(0)
    }

    // ========== execute Failure Tests ==========

    @Test
    fun `execute should return null on exception`() = runTest {
        val result = circuitBreaker.execute<Any> { throw RuntimeException("Error") }
        assertThat(result).isNull()
    }

    @Test
    fun `execute should increment failure count on exception`() = runTest {
        circuitBreaker.execute<Any> { throw RuntimeException("Error") }
        assertThat(circuitBreaker.getFailureCount()).isEqualTo(1)
    }

    @Test
    fun `execute should trip circuit after threshold failures`() = runTest {
        repeat(3) {
            circuitBreaker.execute<Any> { throw RuntimeException("Error") }
        }
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN)
    }

    // ========== State Transition Tests ==========

    @Test
    fun `circuit should transition to OPEN after threshold`() = runTest {
        repeat(3) {
            circuitBreaker.execute<Any> { throw RuntimeException("Error") }
        }
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN)
    }

    @Test
    fun `circuit should stay OPEN before reset timeout`() = runTest {
        // Trip the circuit
        repeat(3) {
            circuitBreaker.execute<Any> { throw RuntimeException("Error") }
        }
        
        // Should be OPEN
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN)
        
        // Try to execute - should fail fast without calling the block
        var blockCalled = false
        circuitBreaker.execute {
            blockCalled = true
            "Result"
        }
        
        assertThat(blockCalled).isFalse()
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN)
    }

    @Test
    fun `reset should clear all state`() = runTest {
        // Trip the circuit
        repeat(3) {
            circuitBreaker.execute<Any> { throw RuntimeException("Error") }
        }
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN)
        
        // Reset
        circuitBreaker.reset()
        
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED)
        assertThat(circuitBreaker.getFailureCount()).isEqualTo(0)
    }

    @Test
    fun `forceOpen should open circuit immediately`() {
        circuitBreaker.forceOpen()
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN)
    }

    // ========== Stats Tests ==========

    @Test
    fun `getStats should return correct statistics`() {
        val stats = circuitBreaker.getStats()
        
        assertThat(stats.state).isEqualTo(CircuitBreaker.State.CLOSED)
        assertThat(stats.failureCount).isEqualTo(0)
        assertThat(stats.successCount).isEqualTo(0)
        assertThat(stats.failureThreshold).isEqualTo(3)
        assertThat(stats.resetTimeoutMs).isEqualTo(100)
    }

    @Test
    fun `getStats should reflect failures`() = runTest {
        repeat(2) {
            circuitBreaker.execute<Any> { throw RuntimeException("Error") }
        }
        
        val stats = circuitBreaker.getStats()
        
        assertThat(stats.failureCount).isEqualTo(2)
        assertThat(stats.state).isEqualTo(CircuitBreaker.State.CLOSED)
    }
}
