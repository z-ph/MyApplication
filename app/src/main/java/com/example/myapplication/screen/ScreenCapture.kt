package com.example.myapplication.screen

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.projection.MediaProjectionManager
import android.util.DisplayMetrics
import android.view.WindowManager
import com.example.myapplication.utils.Logger
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.ByteArrayOutputStream
import kotlin.coroutines.resume

/**
 * Screen Capture Manager
 * Uses ScreenCaptureService for foreground service requirement
 */
class ScreenCapture(private val context: Context) {

    companion object {
        private const val TAG = "ScreenCapture"
        const val REQUEST_SCREEN_CAPTURE = 1001

        @Volatile
        private var instance: ScreenCapture? = null

        fun getInstance(context: Context): ScreenCapture {
            return instance ?: synchronized(this) {
                instance ?: ScreenCapture(context.applicationContext).also { instance = it }
            }
        }

        fun isProjectionActive(): Boolean {
            return ScreenCaptureService.isRunning
        }
    }

    private val logger = Logger(TAG)

    private val mediaProjectionManager: MediaProjectionManager =
        context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

    private val windowManager: WindowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private val displayMetrics: DisplayMetrics by lazy {
        DisplayMetrics().apply {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getMetrics(this)
        }
    }

    val screenWidth: Int get() = displayMetrics.widthPixels
    val screenHeight: Int get() = displayMetrics.heightPixels

    val isCapturing: Boolean get() = ScreenCaptureService.isRunning

    /**
     * Create screen capture intent to request permission
     */
    fun createCaptureIntent(): Intent {
        return mediaProjectionManager.createScreenCaptureIntent()
    }

    /**
     * Start screen capture using foreground service
     */
    suspend fun startCapture(resultCode: Int, data: Intent): Boolean = suspendCancellableCoroutine { continuation ->
        try {
            logger.d("Starting screen capture service with resultCode=$resultCode")

            // Store data in static variables before starting service
            ScreenCaptureService.pendingResultCode = resultCode
            ScreenCaptureService.pendingResultData = data

            val serviceIntent = Intent(context, ScreenCaptureService::class.java).apply {
                action = ScreenCaptureService.ACTION_START
            }

            context.startForegroundService(serviceIntent)

            // Wait for service to start
            Thread.sleep(800)

            val success = ScreenCaptureService.isRunning
            logger.d("Screen capture service started: $success")
            continuation.resume(success)
        } catch (e: Exception) {
            logger.e("Error starting screen capture service: ${e.message}")
            e.printStackTrace()
            continuation.resume(false)
        }
    }

    /**
     * Capture a single frame from the screen
     */
    suspend fun capture(): Bitmap? = suspendCancellableCoroutine { continuation ->
        val service = ScreenCaptureService.instance

        if (service == null) {
            logger.w("ScreenCaptureService not running")
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }

        try {
            val bitmap = service.capture()
            if (bitmap != null) {
                continuation.resume(bitmap)
            } else {
                // Retry once
                Thread.sleep(100)
                val retryBitmap = service.capture()
                continuation.resume(retryBitmap)
            }
        } catch (e: Exception) {
            logger.e("Error capturing screen: ${e.message}")
            continuation.resume(null)
        }
    }

    /**
     * Stop screen capture
     */
    fun stopCapture() {
        logger.d("Stopping screen capture service")
        val serviceIntent = Intent(context, ScreenCaptureService::class.java).apply {
            action = ScreenCaptureService.ACTION_STOP
        }
        context.startService(serviceIntent)
    }

    /**
     * Check if screen capture permission has been granted
     */
    fun hasCapturePermission(): Boolean {
        return ScreenCaptureService.isRunning
    }

    /**
     * Capture screen and convert to byte array
     */
    suspend fun captureToBytes(format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG, quality: Int = 90): ByteArray? {
        val bitmap = capture() ?: return null

        return try {
            val stream = ByteArrayOutputStream()
            bitmap.compress(format, quality, stream)
            stream.toByteArray()
        } catch (e: Exception) {
            logger.e("Error converting bitmap to bytes: ${e.message}")
            null
        } finally {
            bitmap.recycle()
        }
    }
}
