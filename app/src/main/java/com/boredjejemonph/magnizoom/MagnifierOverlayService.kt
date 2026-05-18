package com.boredjejemonph.magnizoom

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
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
import android.os.HandlerThread
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.runtime.R as LifecycleRuntimeR
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.R as SavedStateR
import com.boredjejemonph.magnizoom.ui.theme.MagniZoomTheme
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

class MagnifierOverlayService : LifecycleService(), SavedStateRegistryOwner {
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private var windowManager: WindowManager? = null
    private var overlayView: ComposeView? = null
    private var overlayParams: WindowManager.LayoutParams? = null
    private var overlayMenuVisible = true
    private var expandedOverlayWidthPx = 0
    private var expandedOverlayHeightPx = 0
    private val captureFrameState = mutableStateOf<Bitmap?>(null)
    private val captureStatusState = mutableStateOf(OverlayCaptureStatus.Idle)
    private val captureModeState = mutableStateOf<OverlayCaptureMode?>(null)
    private val captureMessageState = mutableStateOf("")
    private val lensCenterState = mutableStateOf(Offset.Zero)
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var captureThread: HandlerThread? = null
    private var captureHandler: Handler? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    @Volatile
    private var snapshotPending = false
    private var ignoreNextProjectionStop = false
    private var captureFrameLogged = false
    private var snapshotRequestId = 0

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    override fun onCreate() {
        serviceRunning = true
        savedStateRegistryController.performAttach()
        savedStateRegistryController.performRestore(null)
        super.onCreate()
        publishOverlayActive(false)
        createNotificationChannel()
        startOverlayForeground()

        if (!canDrawOverlaysCompat()) {
            stopSelf()
            return
        }

        showOverlay()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        when (intent?.action) {
            ACTION_CAPTURE_PERMISSION_RESULT -> {
                handleCapturePermissionResult(intent)
                return START_STICKY
            }

            ACTION_CAPTURE_CANCELLED -> {
                captureStatusState.value = OverlayCaptureStatus.Idle
                captureModeState.value = null
                captureMessageState.value = ""
                restoreOverlayVisibility()
                return START_STICKY
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        overlayView?.let { view ->
            runCatching { windowManager?.removeView(view) }
        }
        overlayWindowVisible = false
        overlayView = null
        overlayParams = null
        windowManager = null
        stopScreenCapture(clearFrame = true, returnToOverlayForeground = false)
        serviceRunning = false
        publishOverlayActive(false)
        super.onDestroy()
    }

    private fun showOverlay() {
        if (overlayView != null) return

        val manager = getSystemService(WINDOW_SERVICE) as WindowManager
        windowManager = manager
        overlayWindowVisible = false

        val view = ComposeView(this).apply {
            setTag(LifecycleRuntimeR.id.view_tree_lifecycle_owner, this@MagnifierOverlayService)
            setTag(SavedStateR.id.view_tree_saved_state_registry_owner, this@MagnifierOverlayService)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                MagniZoomTheme(dynamicColor = false) {
                    MagnifierOverlayContent(
                        onMove = ::moveOverlayBy,
                        onResize = ::resizeOverlayBy,
                        onMenuVisibilityChange = ::updateOverlayMenuVisibility,
                        onClose = ::stopSelf,
                        captureFrame = captureFrameState.value,
                        captureStatus = captureStatusState.value,
                        captureMode = captureModeState.value,
                        captureMessage = captureMessageState.value,
                        lensCenter = lensCenterState.value,
                        onCaptureModeSelected = ::requestScreenCapture,
                        onStopCapture = { stopScreenCapture(clearFrame = true) }
                    )
                }
            }
        }

        expandedOverlayWidthPx = dp(EXPANDED_WIDTH_DP)
        expandedOverlayHeightPx = dp(EXPANDED_HEIGHT_DP)

        val params = WindowManager.LayoutParams(
            expandedOverlayWidthPx,
            expandedOverlayHeightPx,
            overlayWindowType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dp(12)
            y = dp(88)
        }

        try {
            manager.addView(view, params)
            overlayParams = params
            overlayView = view
            overlayWindowVisible = true
            Log.d(TAG, "Overlay window added secure=false size=${params.width}x${params.height}")
            updateLensCenter()
            publishOverlayActive(true)
        } catch (_: RuntimeException) {
            overlayWindowVisible = false
            Log.w(TAG, "Unable to add overlay window")
            publishOverlayActive(false)
            stopSelf()
        } catch (_: SecurityException) {
            overlayWindowVisible = false
            Log.w(TAG, "Overlay permission denied while adding overlay window")
            publishOverlayActive(false)
            stopSelf()
        }
    }

    private fun updateOverlayMenuVisibility(visible: Boolean) {
        if (overlayMenuVisible == visible) return

        val params = overlayParams ?: return
        val oldCenterX = params.x + params.width / 2
        val oldCenterY = params.y + params.height / 2
        val collapsedSize = collapsedOverlaySize()
        val newWidth = if (visible) expandedOverlayWidthPx else collapsedSize
        val newHeight = if (visible) expandedOverlayHeightPx else collapsedSize

        overlayMenuVisible = visible
        params.width = newWidth
        params.height = newHeight
        moveOverlayTo(oldCenterX - newWidth / 2, oldCenterY - newHeight / 2)
    }

    private fun moveOverlayBy(deltaX: Int, deltaY: Int) {
        val params = overlayParams ?: return
        moveOverlayTo(params.x + deltaX, params.y + deltaY)
    }

    private fun resizeOverlayBy(deltaX: Int, deltaY: Int) {
        if (!overlayMenuVisible) return

        val params = overlayParams ?: return
        val metrics = resources.displayMetrics
        val aspectRatio = EXPANDED_HEIGHT_DP.toFloat() / EXPANDED_WIDTH_DP.toFloat()
        val minWidth = dp(MIN_EXPANDED_WIDTH_DP)
        val minHeight = dp(MIN_EXPANDED_HEIGHT_DP)
        val maxWidth = metrics.widthPixels.coerceAtLeast(minWidth)
        val maxHeight = metrics.heightPixels.coerceAtLeast(minHeight)
        val widthDelta = if (abs(deltaX) >= abs(deltaY)) {
            deltaX
        } else {
            (deltaY / aspectRatio).roundToInt()
        }

        var newWidth = (params.width + widthDelta).coerceIn(minWidth, maxWidth)
        var newHeight = (newWidth * aspectRatio).roundToInt()
        if (newHeight > maxHeight) {
            newHeight = maxHeight
            newWidth = (newHeight / aspectRatio).roundToInt().coerceAtLeast(minWidth)
        }

        if (newWidth == params.width && newHeight == params.height) return

        expandedOverlayWidthPx = newWidth
        expandedOverlayHeightPx = newHeight
        params.width = newWidth
        params.height = newHeight
        moveOverlayTo(params.x, params.y)
    }

    private fun collapsedOverlaySize(): Int {
        val wheelSize = (min(expandedOverlayWidthPx, expandedOverlayHeightPx) - dp(18))
            .coerceAtLeast(dp(MIN_COLLAPSED_SIZE_DP))
        return (wheelSize * COLLAPSED_LENS_SCALE).roundToInt()
            .coerceAtLeast(dp(MIN_COLLAPSED_SIZE_DP))
    }

    private fun moveOverlayTo(targetX: Int, targetY: Int) {
        val manager = windowManager ?: return
        val view = overlayView ?: return
        val params = overlayParams ?: return
        val metrics = resources.displayMetrics
        val maxX = (metrics.widthPixels - params.width).coerceAtLeast(0)
        val maxY = (metrics.heightPixels - params.height).coerceAtLeast(0)

        params.x = targetX.coerceIn(0, maxX)
        params.y = targetY.coerceIn(0, maxY)
        updateLensCenter()

        runCatching {
            manager.updateViewLayout(view, params)
        }
    }

    private fun updateLensCenter() {
        val params = overlayParams ?: return
        lensCenterState.value = Offset(
            x = params.x + params.width / 2f,
            y = params.y + params.height / 2f
        )
    }

    private fun requestScreenCapture(mode: OverlayCaptureMode) {
        val actualMode = if (
            mode == OverlayCaptureMode.TargetAppRealtime &&
            Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE
        ) {
            OverlayCaptureMode.WholeScreenSnapshot
        } else {
            mode
        }

        if (actualMode == OverlayCaptureMode.WholeScreenSnapshot && canReuseCurrentProjectionForSnapshot()) {
            Log.d(TAG, "Reusing current projection for screen snapshot")
            captureModeState.value = actualMode
            captureStatusState.value = OverlayCaptureStatus.Starting
            captureMessageState.value = "Overlay hides for screen snapshot"
            requestSnapshotFrame()
            return
        }

        stopScreenCapture(clearFrame = true)
        captureModeState.value = actualMode
        captureStatusState.value = OverlayCaptureStatus.Starting
        captureMessageState.value = when (actualMode) {
            OverlayCaptureMode.TargetAppRealtime -> "Choose target app"
            OverlayCaptureMode.WholeScreenRealtime -> "Preparing screen"
            OverlayCaptureMode.WholeScreenSnapshot -> "Overlay hides for screen snapshot"
        }
        if (actualMode == OverlayCaptureMode.WholeScreenSnapshot) {
            Toast.makeText(
                this,
                "Screen Shot hides the overlay briefly, captures the screen once, then shows a frozen zoom.",
                Toast.LENGTH_LONG
            ).show()
        }
        Log.d(TAG, "Requesting capture mode=$actualMode")

        val permissionIntent = ScreenCapturePermissionActivity
            .createIntent(this, actualMode)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(permissionIntent)
    }

    private fun canReuseCurrentProjectionForSnapshot(): Boolean {
        return captureModeState.value == OverlayCaptureMode.WholeScreenSnapshot &&
            mediaProjection != null &&
            virtualDisplay != null &&
            imageReader != null &&
            captureHandler != null
    }

    private fun handleCapturePermissionResult(intent: Intent) {
        val mode = OverlayCaptureMode.fromWireName(intent.getStringExtra(EXTRA_CAPTURE_MODE))
        val resultCode = intent.getIntExtra(EXTRA_CAPTURE_RESULT_CODE, 0)
        val resultData = captureResultData(intent)
        if (resultData == null) {
            captureStatusState.value = OverlayCaptureStatus.Error
            captureMessageState.value = "Capture permission failed"
            restoreOverlayVisibility()
            return
        }

        stopScreenCapture(clearFrame = true)
        captureModeState.value = mode
        captureStatusState.value = OverlayCaptureStatus.Starting
        captureMessageState.value = when (mode) {
            OverlayCaptureMode.TargetAppRealtime -> "Starting live capture"
            OverlayCaptureMode.WholeScreenRealtime -> "Starting screen capture"
            OverlayCaptureMode.WholeScreenSnapshot -> "Hiding overlay for snapshot"
        }
        Log.d(TAG, "Capture permission result mode=$mode resultCode=$resultCode data=true")

        startProjectionForeground()
        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val projection = runCatching { projectionManager.getMediaProjection(resultCode, resultData) }.getOrNull()
        if (projection == null) {
            captureStatusState.value = OverlayCaptureStatus.Error
            captureMessageState.value = "Unable to start capture"
            Log.w(TAG, "Unable to create MediaProjection for mode=$mode")
            restoreOverlayVisibility()
            return
        }

        mediaProjection = projection
        projection.registerCallback(
            object : MediaProjection.Callback() {
                override fun onStop() {
                    mainHandler.post {
                        if (ignoreNextProjectionStop) {
                            ignoreNextProjectionStop = false
                            return@post
                        }
                        stopScreenCapture(clearFrame = false)
                        if (captureStatusState.value == OverlayCaptureStatus.Live) {
                            captureStatusState.value = OverlayCaptureStatus.Idle
                            captureMessageState.value = ""
                        }
                    }
                }
            },
            mainHandler
        )
        startProjectionCapture(mode)
    }

    private fun captureResultData(intent: Intent): Intent? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_CAPTURE_RESULT_DATA, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_CAPTURE_RESULT_DATA)
        }
    }

    private fun startProjectionCapture(mode: OverlayCaptureMode) {
        val projection = mediaProjection ?: return
        val metrics = resources.displayMetrics
        val captureScale = min(1f, MAX_CAPTURE_WIDTH_PX.toFloat() / metrics.widthPixels.toFloat())
        val captureWidth = (metrics.widthPixels * captureScale).toInt().coerceAtLeast(1)
        val captureHeight = (metrics.heightPixels * captureScale).toInt().coerceAtLeast(1)
        val captureDensity = metrics.densityDpi
        val thread = HandlerThread("MagniZoomScreenCapture").also { it.start() }
        val handler = Handler(thread.looper)
        val reader = ImageReader.newInstance(captureWidth, captureHeight, PixelFormat.RGBA_8888, 2)

        captureThread = thread
        captureHandler = handler
        imageReader = reader
        captureFrameLogged = false

        reader.setOnImageAvailableListener(
            { imageReader ->
                val image = imageReader.acquireLatestImage() ?: return@setOnImageAvailableListener
                val shouldUseFrame = mode != OverlayCaptureMode.WholeScreenSnapshot || snapshotPending
                if (!shouldUseFrame) {
                    image.close()
                    return@setOnImageAvailableListener
                }

                val bitmap = runCatching { image.toBitmap() }
                    .onFailure { Log.w(TAG, "Unable to convert capture frame", it) }
                    .getOrNull()
                image.close()
                if (bitmap != null) {
                    mainHandler.post {
                        if (mode == OverlayCaptureMode.WholeScreenSnapshot) {
                            if (!snapshotPending) {
                                bitmap.recycle()
                                return@post
                            }
                            snapshotPending = false
                            val previous = captureFrameState.value
                            captureFrameState.value = bitmap
                            if (previous !== bitmap) {
                                previous?.recycle()
                            }
                            captureStatusState.value = OverlayCaptureStatus.Snapshot
                            captureMessageState.value = "Screen Shot ready"
                            Log.d(TAG, "Screen snapshot frame received ${bitmap.width}x${bitmap.height}")
                            restoreOverlayVisibility()
                            Toast.makeText(
                                this@MagnifierOverlayService,
                                "Screen Shot ready. Tap the camera again to recapture without another prompt.",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            val previous = captureFrameState.value
                            captureFrameState.value = bitmap
                            if (previous !== bitmap) {
                                previous?.recycle()
                            }
                            captureStatusState.value = OverlayCaptureStatus.Live
                            captureMessageState.value = when (mode) {
                                OverlayCaptureMode.TargetAppRealtime -> "Live target app"
                                OverlayCaptureMode.WholeScreenRealtime -> "Live screen"
                                OverlayCaptureMode.WholeScreenSnapshot -> "Screen snapshot"
                            }
                            if (!captureFrameLogged) {
                                captureFrameLogged = true
                                Log.d(TAG, "Live capture frame received mode=$mode ${bitmap.width}x${bitmap.height}")
                            }
                        }
                    }
                }
            },
            handler
        )

        virtualDisplay = runCatching {
            projection.createVirtualDisplay(
                "MagniZoomCapture",
                captureWidth,
                captureHeight,
                captureDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                reader.surface,
                null,
                handler
            )
        }.getOrNull()

        if (virtualDisplay == null) {
            captureStatusState.value = OverlayCaptureStatus.Error
            captureMessageState.value = "Capture display failed"
            Log.w(TAG, "Virtual display failed for mode=$mode")
            restoreOverlayVisibility()
            stopScreenCapture(clearFrame = false)
        } else {
            Log.d(TAG, "Virtual display started mode=$mode size=${captureWidth}x$captureHeight")
            if (mode == OverlayCaptureMode.WholeScreenSnapshot) {
                requestSnapshotFrame()
            }
        }
    }

    private fun requestSnapshotFrame() {
        val requestId = ++snapshotRequestId
        snapshotPending = false
        runCatching { imageReader?.acquireLatestImage()?.close() }
        hideOverlayForSnapshot()
        snapshotPending = true
        mainHandler.postDelayed({
            if (requestId == snapshotRequestId && snapshotPending) {
                snapshotPending = false
                restoreOverlayVisibility()
                releaseProjectionResources(stopProjection = true, returnToOverlayForeground = true)
                captureStatusState.value = OverlayCaptureStatus.Error
                captureMessageState.value = "Snapshot timed out"
            }
        }, SNAPSHOT_TIMEOUT_MS)
    }

    private fun hideOverlayForSnapshot() {
        overlayView?.visibility = View.INVISIBLE
    }

    private fun restoreOverlayVisibility() {
        overlayView?.visibility = View.VISIBLE
    }

    private fun stopScreenCapture(clearFrame: Boolean, returnToOverlayForeground: Boolean = true) {
        snapshotPending = false
        snapshotRequestId++
        releaseProjectionResources(
            stopProjection = true,
            returnToOverlayForeground = returnToOverlayForeground
        )
        restoreOverlayVisibility()
        captureStatusState.value = OverlayCaptureStatus.Idle
        captureModeState.value = null
        captureMessageState.value = ""
        if (clearFrame) {
            captureFrameState.value?.recycle()
            captureFrameState.value = null
        }
    }

    private fun releaseProjectionResources(
        stopProjection: Boolean,
        returnToOverlayForeground: Boolean = true
    ) {
        runCatching { virtualDisplay?.release() }
        virtualDisplay = null
        runCatching { imageReader?.close() }
        imageReader = null
        captureHandler = null
        runCatching { captureThread?.quitSafely() }
        captureThread = null
        if (stopProjection) {
            if (mediaProjection != null) {
                ignoreNextProjectionStop = true
                if (returnToOverlayForeground && serviceRunning) {
                    startOverlayForeground()
                }
            }
            runCatching { mediaProjection?.stop() }
            mediaProjection = null
        }
    }

    private fun Image.toBitmap(): Bitmap {
        val crop = cropRect
        val outputWidth = crop.width()
        val outputHeight = crop.height()
        require(outputWidth > 0 && outputHeight > 0) {
            "Invalid image crop: $crop"
        }

        val plane = planes[0]
        val pixelStride = plane.pixelStride
        val rowStride = plane.rowStride
        val source = plane.buffer.duplicate()
        val sourceLimit = source.limit()

        require(pixelStride >= RGBA_BYTES_PER_PIXEL) {
            "Unsupported pixel stride: $pixelStride"
        }

        if (
            pixelStride == RGBA_BYTES_PER_PIXEL &&
            rowStride == outputWidth * RGBA_BYTES_PER_PIXEL &&
            crop.left == 0 &&
            crop.top == 0
        ) {
            source.rewind()
            return Bitmap.createBitmap(outputWidth, outputHeight, Bitmap.Config.ARGB_8888).also {
                it.copyPixelsFromBuffer(source)
            }
        }

        val pixels = IntArray(outputWidth * outputHeight)
        for (row in 0 until outputHeight) {
            val sourceRowStart = (crop.top + row) * rowStride + crop.left * pixelStride
            for (column in 0 until outputWidth) {
                val pixelOffset = sourceRowStart + column * pixelStride
                require(pixelOffset >= 0 && pixelOffset + RGBA_BYTES_PER_PIXEL <= sourceLimit) {
                    "Capture pixel outside buffer: row=$row column=$column offset=$pixelOffset limit=$sourceLimit"
                }

                val red = source.get(pixelOffset).toInt() and 0xFF
                val green = source.get(pixelOffset + 1).toInt() and 0xFF
                val blue = source.get(pixelOffset + 2).toInt() and 0xFF
                val alpha = source.get(pixelOffset + 3).toInt() and 0xFF
                pixels[row * outputWidth + column] =
                    (alpha shl 24) or (red shl 16) or (green shl 8) or blue
            }
        }

        return Bitmap.createBitmap(outputWidth, outputHeight, Bitmap.Config.ARGB_8888).also {
            it.setPixels(pixels, 0, outputWidth, 0, 0, outputWidth, outputHeight)
        }
    }

    private fun startOverlayForeground() {
        ServiceCompat.startForeground(this, NOTIFICATION_ID, buildNotification(), overlayForegroundType())
    }

    private fun startProjectionForeground() {
        ServiceCompat.startForeground(this, NOTIFICATION_ID, buildNotification(), projectionForegroundType())
    }

    private fun overlayForegroundType(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
    }

    private fun projectionForegroundType(): Int {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE ->
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE or ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION

            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ->
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION

            else -> 0
        }
    }

    private fun canDrawOverlaysCompat(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this)
    }

    @Suppress("DEPRECATION")
    private fun overlayWindowType(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            "MagniZoom overlay",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows the floating MagniZoom magnifier overlay."
            setShowBadge(false)
        }

        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java)
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, MagnifierOverlayService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("MagniZoom overlay")
            .setContentText("Floating magnifier is running.")
            .setContentIntent(openAppPendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(R.drawable.ic_launcher_foreground, "Stop", stopPendingIntent)
            .build()
    }

    private fun publishOverlayActive(active: Boolean) {
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(PREF_OVERLAY_ACTIVE, active)
            .apply()

        sendBroadcast(
            Intent(ACTION_STATE_CHANGED)
                .setPackage(packageName)
                .putExtra(EXTRA_ACTIVE, active)
        )
    }

    companion object {
        const val ACTION_STATE_CHANGED = "com.boredjejemonph.magnizoom.action.OVERLAY_STATE_CHANGED"
        const val ACTION_CAPTURE_PERMISSION_RESULT = "com.boredjejemonph.magnizoom.action.CAPTURE_PERMISSION_RESULT"
        const val ACTION_CAPTURE_CANCELLED = "com.boredjejemonph.magnizoom.action.CAPTURE_CANCELLED"
        const val EXTRA_ACTIVE = "com.boredjejemonph.magnizoom.extra.OVERLAY_ACTIVE"
        const val EXTRA_CAPTURE_MODE = "com.boredjejemonph.magnizoom.extra.CAPTURE_MODE"
        const val EXTRA_CAPTURE_RESULT_CODE = "com.boredjejemonph.magnizoom.extra.CAPTURE_RESULT_CODE"
        const val EXTRA_CAPTURE_RESULT_DATA = "com.boredjejemonph.magnizoom.extra.CAPTURE_RESULT_DATA"

        private const val PREFS_NAME = "magnizoom_overlay_state"
        private const val PREF_OVERLAY_ACTIVE = "overlay_active"
        private const val CHANNEL_ID = "magnizoom_overlay"
        private const val NOTIFICATION_ID = 1001
        private const val TAG = "MagniZoomOverlay"
        private const val ACTION_STOP = "com.boredjejemonph.magnizoom.action.STOP_OVERLAY"
        private const val SNAPSHOT_TIMEOUT_MS = 1800L
        private const val RGBA_BYTES_PER_PIXEL = 4
        private const val MAX_CAPTURE_WIDTH_PX = 720
        private const val MIN_EXPANDED_WIDTH_DP = 150
        private const val MIN_EXPANDED_HEIGHT_DP = 157
        private const val EXPANDED_WIDTH_DP = 206
        private const val EXPANDED_HEIGHT_DP = 216
        private const val MIN_COLLAPSED_SIZE_DP = 70
        private const val COLLAPSED_LENS_SCALE = 0.48f
        @Volatile
        private var serviceRunning = false
        @Volatile
        private var overlayWindowVisible = false

        fun isOverlayActive(context: Context): Boolean {
            val markedActive = context
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(PREF_OVERLAY_ACTIVE, false)
            val active = markedActive && serviceRunning && overlayWindowVisible
            if (markedActive && !active) {
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean(PREF_OVERLAY_ACTIVE, false)
                    .apply()
            }
            return active
        }
    }
}
