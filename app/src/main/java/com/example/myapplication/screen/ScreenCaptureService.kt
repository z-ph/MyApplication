package com.example.myapplication.screen

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.example.myapplication.R
import com.example.myapplication.utils.Logger
import java.nio.ByteBuffer

/**
 * Foreground service for screen capture
 * Required for Android 10+ to use MediaProjection
 */
class ScreenCaptureService : Service() {

    companion object {
        private const val TAG = "ScreenCaptureService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "screen_capture_channel"

        const val ACTION_START = "com.example.myapplication.START_CAPTURE"
        const val ACTION_STOP = "com.example.myapplication.STOP_CAPTURE"

        // Store projection data statically to avoid Intent size limits
        @Volatile
        var pendingResultCode: Int = -1
        @Volatile
        var pendingResultData: Intent? = null

        @Volatile
        var isRunning = false
            private set

        @Volatile
        var instance: ScreenCaptureService? = null
            private set
    }

    private val logger = Logger(TAG)

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null

    private val windowManager: WindowManager by lazy {
        getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    private val displayMetrics: DisplayMetrics by lazy {
        DisplayMetrics().apply {
            windowManager.defaultDisplay.getMetrics(this)
        }
    }

    val screenWidth: Int get() = displayMetrics.widthPixels
    val screenHeight: Int get() = displayMetrics.heightPixels

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannel()
        logger.d("Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        logger.d("onStartCommand: action=${intent?.action}")

        // Must call startForeground immediately when started with startForegroundService
        startForegroundWithNotification()

        when (intent?.action) {
            ACTION_START -> {
                val resultCode = pendingResultCode
                val data = pendingResultData

                logger.d("Pending data: resultCode=$resultCode, data=$data")

                // RESULT_OK = -1 in Android
                if (resultCode == android.app.Activity.RESULT_OK && data != null) {
                    startProjection(resultCode, data)
                    isRunning = true
                    logger.d("Projection started, isRunning=$isRunning")
                    // Clear pending data
                    pendingResultCode = -1
                    pendingResultData = null
                } else {
                    logger.e("Invalid pending data: resultCode=$resultCode, data=$data")
                }
            }
            ACTION_STOP -> {
                stopProjection()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        logger.d("Service destroying")
        stopProjection()
        isRunning = false
        instance = null
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Screen Capture",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Screen capture service notification"
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun startForegroundWithNotification() {
        val notification = createNotification()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        logger.d("Started as foreground service")
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AI Automation Assistant")
            .setContentText("Screen capture is active")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    private fun startProjection(resultCode: Int, data: Intent) {
        try {
            logger.d("Starting projection with resultCode=$resultCode")

            val mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data)

            if (mediaProjection == null) {
                logger.e("Failed to get media projection")
                return
            }

            // Register callback before starting capture (required by Android)
            mediaProjection?.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() {
                    logger.d("MediaProjection stopped")
                    stopProjection()
                }
            }, null)

            initImageReader()
            logger.d("Projection started successfully")

        } catch (e: Exception) {
            logger.e("Error starting projection: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun initImageReader() {
        val width = screenWidth
        val height = screenHeight

        logger.d("Initializing image reader: ${width}x$height")

        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)

        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "ScreenCapture",
            width, height,
            displayMetrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface,
            null,
            null
        )

        logger.d("VirtualDisplay created: ${virtualDisplay != null}")
    }

    private fun stopProjection() {
        logger.d("Stopping projection")

        virtualDisplay?.release()
        virtualDisplay = null

        imageReader?.close()
        imageReader = null

        mediaProjection?.stop()
        mediaProjection = null
    }

    fun capture(): Bitmap? {
        val currentImageReader = imageReader ?: return null

        var image: Image? = null
        try {
            image = currentImageReader.acquireLatestImage() ?: currentImageReader.acquireNextImage()

            if (image != null) {
                return imageToBitmap(image)
            }
        } catch (e: Exception) {
            logger.e("Error capturing image: ${e.message}")
        } finally {
            image?.close()
        }

        return null
    }

    private fun imageToBitmap(image: Image): Bitmap? {
        try {
            val planes = image.planes
            val buffer: ByteBuffer = planes[0].buffer

            val pixelStride = planes[0].pixelStride
            val rowStride = planes[0].rowStride
            val rowPadding = rowStride - pixelStride * image.width

            val bitmap = Bitmap.createBitmap(
                image.width + rowPadding / pixelStride,
                image.height,
                Bitmap.Config.ARGB_8888
            )

            bitmap.copyPixelsFromBuffer(buffer)

            return if (rowPadding != 0) {
                Bitmap.createBitmap(bitmap, 0, 0, image.width, image.height)
            } else {
                bitmap
            }
        } catch (e: Exception) {
            logger.e("Error converting image to bitmap: ${e.message}")
            return null
        }
    }
}
