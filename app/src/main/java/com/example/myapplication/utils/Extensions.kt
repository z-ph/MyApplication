package com.example.myapplication.utils

import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Base64
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.flow.StateFlow
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utility extension functions
 */

// Bitmap extensions

/**
 * Convert bitmap to base64 string
 */
fun Bitmap.toBase64(format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG, quality: Int = 85): String {
    val stream = ByteArrayOutputStream()
    this.compress(format, quality, stream)
    val bytes = stream.toByteArray()
    return Base64.encodeToString(bytes, Base64.NO_WRAP)
}

/**
 * Get bitmap size in bytes
 */
val Bitmap.sizeInBytes: Int
    get() {
        val stream = ByteArrayOutputStream()
        this.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        return stream.size()
    }

/**
 * Get bitmap dimensions as string
 */
val Bitmap.dimensionsString: String
    get() = "${width}x$height"

// AccessibilityNodeInfo extensions

/**
 * Get node path as string
 */
fun AccessibilityNodeInfo.getPath(): String {
    val path = mutableListOf<String>()
    var node: AccessibilityNodeInfo? = this

    while (node != null) {
        val identifier = node.className?.toString()
            ?: node.viewIdResourceName
            ?: node.text?.toString()?.take(20)
            ?: "node"

        path.add(0, identifier)
        node = node.parent
    }

    return path.joinToString(" > ")
}

/**
 * Check if node is visible
 */
val AccessibilityNodeInfo.isVisible: Boolean
    get() = this.isVisibleToUser && this.boundsInScreen.width() > 0 && this.boundsInScreen.height() > 0

/**
 * Get node bounds
 */
val AccessibilityNodeInfo.boundsInScreen: Rect
    get() {
        val bounds = Rect()
        this.getBoundsInScreen(bounds)
        return bounds
    }

/**
 * Get center point of node
 */
val AccessibilityNodeInfo.center: Pair<Float, Float>
    get() {
        val bounds = boundsInScreen
        return bounds.centerX().toFloat() to bounds.centerY().toFloat()
    }

// Rect extensions

/**
 * Get center point of rect
 */
val Rect.center: Pair<Float, Float>
    get() = centerX().toFloat() to centerY().toFloat()

/**
 * Get rect dimensions as string
 */
val Rect.dimensionsString: String
    get() = "${width()}x${height()}"

// String extensions

/**
 * Check if string is not null and not blank
 */
fun String?.isNotNullOrBlank(): Boolean = !this.isNullOrBlank()

/**
 * Check if string is null or blank
 */
fun String?.isNullOrBlank(): Boolean = this.isNullOrBlank()

/**
 * Truncate string to max length
 */
fun String.truncate(maxLength: Int): String {
    return if (this.length <= maxLength) {
        this
    } else {
        this.take(maxLength) + "..."
    }
}

/**
 * Format timestamp to readable date/time
 */
fun Long.toDateTime(format: String = "yyyy-MM-dd HH:mm:ss"): String {
    val sdf = SimpleDateFormat(format, Locale.getDefault())
    return sdf.format(Date(this))
}

/**
 * Format timestamp to time only
 */
fun Long.toTime(): String = this.toDateTime("HH:mm:ss")

/**
 * Format file size
 */
fun Long.formatFileSize(): String {
    if (this < 1024) return "$this B"
    if (this < 1024 * 1024) return "${this / 1024} KB"
    if (this < 1024 * 1024 * 1024) return "${this / (1024 * 1024)} MB"
    return "${this / (1024 * 1024 * 1024)} GB"
}

// Collection extensions

/**
 * Chunk list into smaller lists
 */
fun <T> List<T>.chunked(size: Int): List<List<T>> {
    val chunks = mutableListOf<List<T>>()
    var index = 0

    while (index < this.size) {
        val end = minOf(index + size, this.size)
        chunks.add(this.subList(index, end))
        index = end
    }

    return chunks
}

/**
 * Find first item matching predicate or return default
 */
inline fun <T> List<T>.firstOrNullOrDefault(default: T, predicate: (T) -> Boolean): T {
    return firstOrNull(predicate) ?: default
}

// Number extensions

/**
 * Convert dp to px
 */
fun Int.toPx(): Int = (this * android.content.res.Resources.getSystem().displayMetrics.density).toInt()

/**
 * Convert px to dp
 */
fun Int.toDp(): Int = (this / android.content.res.Resources.getSystem().displayMetrics.density).toInt()

/**
 * Convert float to int with rounding
 */
fun Float.roundToInt(): Int = kotlin.math.round(this).toInt()

// Boolean extensions

/**
 * Convert boolean to int (1 or 0)
 */
fun Boolean.toInt(): Int = if (this) 1 else 0

/**
 * Invert boolean
 */
fun Boolean.not(): Boolean = !this

// StateFlow extensions

/**
 * Collect latest value once
 */
suspend fun <T> StateFlow<T>.collectLatestOnce(action: (T) -> Unit) {
    var hasEmitted = false
    this.collect { value ->
        if (!hasEmitted) {
            hasEmitted = true
            action(value)
        }
    }
}

// Exception extensions

/**
 * Get root cause of exception
 */
val Throwable.rootCause: Throwable
    get() {
        var cause = this
        while (cause.cause != null) {
            cause = cause.cause!!
        }
        return cause
    }

/**
 * Get exception stack trace as string
 */
val Throwable.stackTraceString: String
    get() {
        val sw = java.io.StringWriter()
        val pw = java.io.PrintWriter(sw)
        this.printStackTrace(pw)
        return sw.toString()
    }

// Retry utilities

/**
 * Retry operation with exponential backoff
 */
suspend fun <T> retryWithBackoff(
    times: Int = 3,
    initialDelayMs: Long = 1000,
    maxDelayMs: Long = 10000,
    block: suspend () -> T
): T {
    var currentDelay = initialDelayMs
    var lastException: Exception? = null

    repeat(times) { attempt ->
        try {
            return block()
        } catch (e: Exception) {
            lastException = e
            if (attempt < times - 1) {
                kotlinx.coroutines.delay(currentDelay)
                currentDelay = minOf(currentDelay * 2, maxDelayMs)
            }
        }
    }

    throw lastException!!
}

// Memory utilities

/**
 * Get available memory in bytes
 */
fun getAvailableMemoryBytes(): Long {
    val runtime = Runtime.getRuntime()
    return runtime.maxMemory() - (runtime.totalMemory() - runtime.freeMemory())
}

/**
 * Get used memory in bytes
 */
fun getUsedMemoryBytes(): Long {
    val runtime = Runtime.getRuntime()
    return runtime.totalMemory() - runtime.freeMemory()
}

/**
 * Format memory size
 */
fun Long.formatMemorySize(): String {
    return this.formatFileSize()
}
