package com.boredjejemonph.magnizoom

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Size as AndroidSize
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brightness5
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.InvertColors
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.boredjejemonph.magnizoom.ui.theme.MagniZoomTheme
import java.util.Locale
import java.util.concurrent.Executors
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

private val AppBlack = Color(0xFF020407)
private val AppPanel = Color(0xE50A0F14)
private val AppPanelHigh = Color(0xF0121820)
private val AppPanelLow = Color(0xDB05080D)
private val ElectricBlue = Color(0xFF2EA7FF)
private val ElectricBlueSoft = Color(0xFF72C7FF)
private val Rim = Color(0xFFB8C6D6)
private val RimDim = Color(0xFF4D5B68)
private val TextPrimary = Color(0xFFF7FAFF)
private val TextSecondary = Color(0xFFB9C3D1)
private val TextMuted = Color(0xFF7F8B9A)
private const val MIN_ZOOM = 1f
private const val MAX_ZOOM = 10f
private const val CAMERA_FRAME_INTERVAL_MS = 33L

private enum class AppTab(val label: String, val icon: ImageVector) {
    Magnifier("Magnifier", Icons.Filled.ZoomIn),
    History("History", Icons.Filled.History),
    Settings("Settings", Icons.Filled.Settings)
}

private enum class LensEffect(val label: String, val icon: ImageVector) {
    Standard("Standard", Icons.Filled.AutoAwesome),
    TextBoost("Text Boost", Icons.Filled.TextFields),
    EdgeDetail("Edge Detail", Icons.Filled.CenterFocusStrong),
    LowLight("Low Light", Icons.Filled.Brightness5),
    ColorDetail("Color Detail", Icons.Filled.LightMode),
    Monochrome("Monochrome", Icons.Filled.Tune),
    Invert("Invert", Icons.Filled.InvertColors),
    HighContrast("High Contrast", Icons.Filled.Contrast)
}

private val EssentialLensEffects = listOf(
    LensEffect.Standard,
    LensEffect.TextBoost,
    LensEffect.EdgeDetail,
    LensEffect.LowLight,
    LensEffect.ColorDetail,
    LensEffect.Monochrome,
    LensEffect.Invert,
    LensEffect.HighContrast
)

private val StackableLensEffects = EssentialLensEffects.filterNot { it == LensEffect.Standard }

private enum class ClarityMode(
    val label: String,
    val buttonLabel: String,
    val icon: ImageVector
) {
    Off("Off", "Clarity", Icons.Filled.Tune),
    Fast("Fast", "Fast", Icons.Filled.AutoAwesome),
    Sharp("Sharp", "Sharp", Icons.Filled.CenterFocusStrong);

    fun next(): ClarityMode {
        return entries[(entries.indexOf(this) + 1) % entries.size]
    }
}

private enum class DialAction {
    Capture,
    Clarity
}

private val ResizeWindowIcon: ImageVector
    get() {
        if (_resizeWindowIcon != null) return _resizeWindowIcon!!

        _resizeWindowIcon = ImageVector.Builder(
            name = "ResizeWindow",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(802.31f, 802.31f)
                quadToRelative(-7.82f, 7.82f, -17.8f, 7.82f)
                quadToRelative(-9.97f, 0f, -17.54f, -7.82f)
                lineTo(157.69f, 193.03f)
                quadToRelative(-7.43f, -7.18f, -7.29f, -17.14f)
                quadToRelative(0.14f, -9.97f, 7.7f, -17.79f)
                quadToRelative(7.82f, -7.56f, 17.8f, -7.56f)
                quadToRelative(9.97f, 0f, 17.79f, 7.56f)
                lineToRelative(608.62f, 608.87f)
                quadToRelative(7.43f, 7.18f, 7.62f, 17.35f)
                quadToRelative(0.2f, 10.17f, -7.62f, 17.99f)
                close()
                moveTo(400.67f, 802.31f)
                quadToRelative(-7.57f, 7.82f, -17.54f, 7.48f)
                quadToRelative(-9.98f, -0.33f, -17.8f, -7.89f)
                lineTo(157.69f, 594.26f)
                quadToRelative(-7.43f, -7.44f, -7.29f, -17.4f)
                quadToRelative(0.14f, -9.96f, 7.7f, -17.53f)
                quadToRelative(7.82f, -7.82f, 17.8f, -7.82f)
                quadToRelative(9.97f, 0f, 17.79f, 7.82f)
                lineToRelative(206.98f, 206.98f)
                quadToRelative(7.43f, 7.43f, 7.62f, 17.8f)
                quadToRelative(0.2f, 10.38f, -7.62f, 18.2f)
                close()
            }
        }.build()

        return _resizeWindowIcon!!
    }

private var _resizeWindowIcon: ImageVector? = null

private data class SavedLens(
    val title: String,
    val effects: Set<LensEffect>,
    val zoom: Float,
    val intensity: Int,
    val date: String,
    val time: String,
    val preview: PreviewKind
)

private enum class PreviewKind {
    Reading,
    Receipt,
    Map,
    Article,
    Photo
}

private data class DialItem(
    val effect: LensEffect?,
    val action: DialAction? = null,
    val label: String,
    val description: String,
    val icon: ImageVector,
    val baseAngleDegrees: Float
)

private fun LensEffect.toDialItem(baseAngleDegrees: Float): DialItem {
    return DialItem(
        effect = this,
        action = null,
        label = label,
        description = effectDescription(),
        icon = icon,
        baseAngleDegrees = baseAngleDegrees
    )
}

private data class DialGuideText(
    val title: String,
    val description: String,
    val icon: ImageVector
)

private fun LensEffect.effectDescription(): String {
    return when (this) {
        LensEffect.Standard -> "Clear all filters"
        LensEffect.TextBoost -> "Sharper text and receipts"
        LensEffect.EdgeDetail -> "Clarifies outlines"
        LensEffect.LowLight -> "Brightens dim details"
        LensEffect.ColorDetail -> "Separates colors"
        LensEffect.Monochrome -> "Removes color distraction"
        LensEffect.Invert -> "Reverses tones"
        LensEffect.HighContrast -> "Stronger light and dark"
    }
}

private fun dialGuideText(
    item: DialItem,
    clarityMode: ClarityMode,
    captureActive: Boolean
): DialGuideText {
    val effect = item.effect
    if (effect != null) {
        return DialGuideText(
            title = effect.label,
            description = effect.effectDescription(),
            icon = effect.icon
        )
    }

    return when (item.action) {
        DialAction.Capture -> DialGuideText(
            title = if (captureActive) "Capture Active" else "Capture",
            description = if (captureActive) "Tap to stop or update" else "Choose live or snapshot",
            icon = item.icon
        )

        DialAction.Clarity -> DialGuideText(
            title = "Clarity ${clarityMode.label}",
            description = "Cycle sharpening strength",
            icon = clarityMode.icon
        )

        null -> DialGuideText(
            title = item.label,
            description = item.description,
            icon = item.icon
        )
    }
}

private fun toggleLensEffect(activeEffects: Set<LensEffect>, effect: LensEffect): Set<LensEffect> {
    if (effect == LensEffect.Standard) return emptySet()
    return if (effect in activeEffects) {
        activeEffects - effect
    } else {
        activeEffects + effect
    }
}

private fun activeEffectLabel(activeEffects: Set<LensEffect>): String {
    val active = StackableLensEffects.filter { it in activeEffects }
    return when {
        active.isEmpty() -> LensEffect.Standard.label
        active.size <= 2 -> active.joinToString(" + ") { it.label }
        else -> "${active.take(2).joinToString(" + ") { it.label }} +${active.size - 2}"
    }
}

private fun activeEffectIcon(activeEffects: Set<LensEffect>): ImageVector {
    return StackableLensEffects.firstOrNull { it in activeEffects }?.icon ?: LensEffect.Standard.icon
}

private fun buildMagnifierDialItems(
    includeCaptureAction: Boolean,
    includeEnhanceAction: Boolean
): List<DialItem> {
    val items = buildList {
        EssentialLensEffects.forEach { add(it.toDialItem(0f)) }
        if (includeCaptureAction) {
            add(
                DialItem(
                    effect = null,
                    action = DialAction.Capture,
                    label = "Capture",
                    description = "Choose live or snapshot",
                    icon = Icons.Filled.Videocam,
                    baseAngleDegrees = 0f
                )
            )
        }
        if (includeEnhanceAction) {
            add(
                DialItem(
                    effect = null,
                    action = DialAction.Clarity,
                    label = "Clarity",
                    description = "Cycle sharpening strength",
                    icon = Icons.Filled.AutoAwesome,
                    baseAngleDegrees = 0f
                )
            )
        }
    }
    val angleStep = 360f / items.size.toFloat()
    return items.mapIndexed { index, item ->
        item.copy(baseAngleDegrees = -90f + index * angleStep)
    }
}

private fun pointedDialItem(dialItems: List<DialItem>, rotationDegrees: Float): DialItem? {
    return dialItems.minByOrNull { item ->
        kotlin.math.abs(shortestAngleDeltaDegrees(item.baseAngleDegrees + rotationDegrees, -90f))
    }
}

@Composable
private fun Modifier.twoFingerZoomInput(
    enabled: Boolean,
    zoom: Float,
    onZoomChange: (Float) -> Unit
): Modifier {
    val latestZoom by rememberUpdatedState(zoom)
    val latestOnZoomChange by rememberUpdatedState(onZoomChange)
    return if (!enabled) {
        this
    } else {
        pointerInput(enabled) {
            awaitEachGesture {
                var gestureZoom = latestZoom
                var previousDistance: Float? = null

                while (true) {
                    val event = awaitPointerEvent()
                    val pressed = event.changes.filter { it.pressed }
                    if (pressed.isEmpty()) break

                    if (pressed.size < 2) {
                        previousDistance = null
                        continue
                    }

                    val distance = (pressed[0].position - pressed[1].position).getDistance()
                    val lastDistance = previousDistance
                    if (lastDistance != null && lastDistance > 0f && distance > 0f) {
                        val pinchScale = distance / lastDistance
                        if (pinchScale.isFinite()) {
                            gestureZoom = (gestureZoom * pinchScale).coerceIn(MIN_ZOOM, MAX_ZOOM)
                            latestOnZoomChange(gestureZoom)
                        }
                        pressed.forEach { it.consume() }
                    }
                    previousDistance = distance
                }
            }
        }
    }
}

@Composable
fun MagniZoomApp(
    modifier: Modifier = Modifier,
    overlayActive: Boolean = false,
    onStartOverlay: () -> Unit = {}
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(AppTab.Magnifier) }
    var activeEffects by remember { mutableStateOf<Set<LensEffect>>(emptySet()) }
    var zoom by remember { mutableStateOf(2.0f) }
    var intensity by remember { mutableStateOf(0.75f) }
    var cameraModeEnabled by remember { mutableStateOf(false) }
    var clarityMode by remember { mutableStateOf(ClarityMode.Off) }
    var cameraPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        cameraPermissionGranted = granted
        cameraModeEnabled = granted
        if (!granted) {
            Toast.makeText(context, "Camera permission is required for live magnifier mode.", Toast.LENGTH_LONG).show()
        }
    }
    LaunchedEffect(overlayActive) {
        if (overlayActive) {
            cameraModeEnabled = false
        }
    }

    val cameraActive = !overlayActive && cameraModeEnabled && cameraPermissionGranted
    val cameraFrame = rememberCameraFrame(cameraActive)

    val savedLens = remember {
        listOf(
            SavedLens("Reading Mode", setOf(LensEffect.TextBoost, LensEffect.HighContrast), 2.0f, 75, "May 16, 2026", "10:30 AM", PreviewKind.Reading),
            SavedLens("Receipt Zoom", setOf(LensEffect.HighContrast, LensEffect.Invert), 2.5f, 60, "May 15, 2026", "7:48 PM", PreviewKind.Receipt),
            SavedLens("Map Labels", setOf(LensEffect.EdgeDetail, LensEffect.ColorDetail), 3.0f, 70, "May 15, 2026", "5:12 PM", PreviewKind.Map),
            SavedLens("Article Focus", setOf(LensEffect.Monochrome, LensEffect.TextBoost), 2.2f, 65, "May 14, 2026", "11:03 AM", PreviewKind.Article),
            SavedLens("Photo Detail", setOf(LensEffect.ColorDetail, LensEffect.LowLight), 2.8f, 80, "May 13, 2026", "9:22 PM", PreviewKind.Photo)
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .twoFingerZoomInput(
                enabled = selectedTab == AppTab.Magnifier && !overlayActive,
                zoom = zoom,
                onZoomChange = { zoom = it }
            )
    ) {
        if (cameraActive) {
            CameraFrameBackdrop(
                frame = cameraFrame,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            DarkAcrylicBackground()
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (selectedTab) {
                    AppTab.Magnifier -> MagnifierPage(
                        activeEffects = activeEffects,
                        onEffectSelected = { activeEffects = toggleLensEffect(activeEffects, it) },
                        zoom = zoom,
                        onZoomChange = { zoom = it },
                        intensity = intensity,
                        onIntensityChange = { intensity = it },
                        clarityMode = clarityMode,
                        onClarityModeChange = { clarityMode = it },
                        onStartOverlay = onStartOverlay,
                        overlayActive = overlayActive,
                        cameraActive = cameraActive,
                        cameraFrame = cameraFrame,
                        onCaptureToggle = {
                            if (!overlayActive) {
                                if (cameraActive) {
                                    cameraModeEnabled = false
                                } else if (cameraPermissionGranted) {
                                    cameraModeEnabled = true
                                } else {
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            }
                        },
                        onHistory = { selectedTab = AppTab.History },
                        onSettings = { selectedTab = AppTab.Settings }
                    )

                    AppTab.History -> HistoryPage(savedLens = savedLens)
                    AppTab.Settings -> SettingsPage(
                        activeEffects = activeEffects,
                        onEffectSelected = { activeEffects = toggleLensEffect(activeEffects, it) },
                        zoom = zoom,
                        onZoomChange = { zoom = it },
                        intensity = intensity,
                        onIntensityChange = { intensity = it },
                        clarityMode = clarityMode,
                        onClarityModeChange = { clarityMode = it }
                    )
                }
            }

            if (selectedTab != AppTab.Magnifier) {
                BottomNavDock(
                    selectedTab = selectedTab,
                    onSelected = { selectedTab = it },
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
        }
    }
}

@Composable
fun MagnifierOverlayContent(
    modifier: Modifier = Modifier,
    onMove: (Int, Int) -> Unit = { _, _ -> },
    onResize: (Int, Int) -> Unit = { _, _ -> },
    onMenuVisibilityChange: (Boolean) -> Unit = {},
    onClose: () -> Unit = {},
    captureFrame: Bitmap? = null,
    captureStatus: OverlayCaptureStatus = OverlayCaptureStatus.Idle,
    captureMode: OverlayCaptureMode? = null,
    captureMessage: String = "",
    lensCenter: Offset = Offset.Zero,
    onCaptureModeSelected: (OverlayCaptureMode) -> Unit = {},
    onStopCapture: () -> Unit = {}
) {
    var activeEffects by remember { mutableStateOf<Set<LensEffect>>(emptySet()) }
    var menuVisible by remember { mutableStateOf(true) }
    var capturePickerVisible by remember { mutableStateOf(false) }
    var clarityMode by remember { mutableStateOf(ClarityMode.Off) }
    val captureBusy = captureStatus == OverlayCaptureStatus.Live ||
        captureStatus == OverlayCaptureStatus.Starting
    val snapshotReady = captureStatus == OverlayCaptureStatus.Snapshot
    val captureActive = captureBusy || snapshotReady
    val statusCaptureDescription = when {
        snapshotReady && captureMode == OverlayCaptureMode.WholeScreenSnapshot -> "Take another Screen Shot"
        captureBusy -> "Stop capture"
        else -> "Capture active"
    }
    val closeProgress by animateFloatAsState(
        targetValue = if (menuVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 220),
        label = "overlayCloseProgress"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .semantics {
                contentDescription = "Floating MagniZoom overlay"
            },
        contentAlignment = Alignment.Center
    ) {
        val shortSide = min(maxWidth.value, maxHeight.value).dp
        val resizedWheelSize = if (menuVisible) {
            (shortSide - 18.dp).coerceAtLeast(92.dp)
        } else {
            shortSide.coerceAtLeast(92.dp)
        }
        val controlWheelSize = resizedWheelSize
        val lensSize = if (menuVisible) {
            (controlWheelSize * 0.48f).coerceAtLeast(70.dp)
        } else {
            resizedWheelSize.coerceAtLeast(70.dp)
        }
        val contentSize = controlWheelSize
        val closeOffset = controlWheelSize * 0.33f
        val resizeOffset = contentSize * 0.42f

        EffectWheel(
            activeEffects = activeEffects,
            onEffectSelected = { activeEffects = toggleLensEffect(activeEffects, it) },
            menuToggleEnabled = true,
            menuVisible = menuVisible,
            includeCaptureAction = true,
            includeEnhanceAction = true,
            captureActive = captureActive,
            onCaptureClick = {
                android.util.Log.i(
                    "MagniZoomOverlay",
                    "Capture dial clicked status=$captureStatus mode=$captureMode picker=$capturePickerVisible"
                )
                if (captureBusy) {
                    onStopCapture()
                    capturePickerVisible = false
                } else if (snapshotReady && captureMode == OverlayCaptureMode.WholeScreenSnapshot) {
                    capturePickerVisible = !capturePickerVisible
                } else if (snapshotReady) {
                    onStopCapture()
                    capturePickerVisible = false
                } else {
                    capturePickerVisible = !capturePickerVisible
                }
            },
            lensFrame = captureFrame,
            screenLensCenter = if (captureFrame != null) lensCenter else null,
            lensZoom = 2.6f,
            clarityMode = clarityMode,
            onEnhanceClick = { clarityMode = clarityMode.next() },
            onLensDrag = onMove,
            onLensDoubleTap = {
                val nextVisible = !menuVisible
                menuVisible = nextVisible
                onMenuVisibilityChange(nextVisible)
                if (!nextVisible) capturePickerVisible = false
            },
            controlSize = controlWheelSize,
            lensSize = lensSize,
            smallGuideText = true,
            modifier = Modifier
                .requiredSize(contentSize)
                .align(Alignment.Center)
        )
        if (capturePickerVisible && menuVisible) {
            OverlayCapturePicker(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 4.dp),
                onTargetApp = {
                    android.util.Log.i("MagniZoomOverlay", "Capture picker target app selected")
                    capturePickerVisible = false
                    onCaptureModeSelected(OverlayCaptureMode.TargetAppRealtime)
                },
                onScreenSnapshot = {
                    android.util.Log.i("MagniZoomOverlay", "Capture picker screen snapshot selected")
                    capturePickerVisible = false
                    onCaptureModeSelected(OverlayCaptureMode.WholeScreenSnapshot)
                }
            )
        } else if (captureActive && menuVisible) {
            OverlayCaptureStatusIndicator(
                icon = when (captureMode) {
                    OverlayCaptureMode.WholeScreenRealtime,
                    OverlayCaptureMode.WholeScreenSnapshot -> Icons.Filled.PhotoCamera
                    OverlayCaptureMode.TargetAppRealtime,
                    null -> Icons.Filled.Videocam
                },
                contentDescription = statusCaptureDescription,
                onClick = {
                    android.util.Log.i(
                        "MagniZoomOverlay",
                        "Capture status clicked status=$captureStatus mode=$captureMode"
                    )
                    if (snapshotReady && captureMode == OverlayCaptureMode.WholeScreenSnapshot) {
                        capturePickerVisible = false
                        onCaptureModeSelected(OverlayCaptureMode.WholeScreenSnapshot)
                    } else if (captureBusy) {
                        onStopCapture()
                        capturePickerVisible = false
                    } else if (snapshotReady) {
                        onStopCapture()
                        capturePickerVisible = false
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 4.dp)
            )
        }
        if (closeProgress > 0.01f) {
            RoundIconButton(
                icon = Icons.Filled.Close,
                contentDescription = "Close overlay",
                selected = false,
                onClick = onClose,
                size = 32.dp,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(x = closeOffset, y = -closeOffset)
                    .graphicsLayer {
                        alpha = closeProgress
                        val closeScale = 0.82f + (0.18f * closeProgress)
                        scaleX = closeScale
                        scaleY = closeScale
                    }
            )
            OverlayResizeHandle(
                onResize = onResize,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(x = resizeOffset, y = resizeOffset)
                    .graphicsLayer {
                        alpha = closeProgress
                        val handleScale = 0.82f + (0.18f * closeProgress)
                        scaleX = handleScale
                        scaleY = handleScale
                    }
            )
        }
    }
}

@Composable
private fun OverlayResizeHandle(
    onResize: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .pointerInput(onResize) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onResize(dragAmount.x.roundToInt(), dragAmount.y.roundToInt())
                }
            }
            .semantics {
                contentDescription = "Resize overlay"
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = ResizeWindowIcon,
            contentDescription = null,
            tint = TextPrimary.copy(alpha = 0.92f),
            modifier = Modifier
                .size(24.dp)
                .graphicsLayer {
                    scaleX = -1f
                }
        )
    }
}

@Composable
private fun OverlayCapturePicker(
    modifier: Modifier = Modifier,
    onTargetApp: () -> Unit,
    onScreenSnapshot: () -> Unit
) {
    GlassPanel(
        modifier = modifier
            .width(176.dp)
            .height(54.dp),
        shape = RoundedCornerShape(16.dp),
        selected = true
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 7.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CaptureModeButton(
                label = "App Live",
                icon = Icons.Filled.Videocam,
                contentDescription = "App live realtime capture",
                onClick = onTargetApp,
                modifier = Modifier.weight(1f)
            )
            CaptureModeButton(
                label = "Screen Shot",
                icon = Icons.Filled.PhotoCamera,
                contentDescription = "Screen snapshot hides overlay briefly",
                onClick = onScreenSnapshot,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun CaptureModeButton(
    label: String,
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxHeight()
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        shape = RoundedCornerShape(12.dp),
        color = Color(0xD904080D),
        border = BorderStroke(1.dp, Rim.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = ElectricBlueSoft, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(4.dp))
            Text(
                text = label,
                color = TextPrimary,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun OverlayCaptureStatusIndicator(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassPanel(
        modifier = modifier
            .size(42.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics {
                this.contentDescription = contentDescription
            },
        shape = RoundedCornerShape(14.dp),
        selected = true
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = ElectricBlueSoft,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun DarkAcrylicBackground(modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(AppBlack)
    ) {
        val cell = 26.dp.toPx()
        val columns = (size.width / cell).toInt() + 2
        val rows = (size.height / cell).toInt() + 2
        for (row in 0 until rows) {
            for (column in 0 until columns) {
                val even = (row + column) % 2 == 0
                drawRect(
                    color = if (even) Color(0xFF071018) else Color(0xFF101B26),
                    topLeft = Offset(column * cell, row * cell),
                    size = Size(cell, cell),
                    alpha = if (even) 0.82f else 0.58f
                )
            }
        }
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(ElectricBlue.copy(alpha = 0.28f), Color.Transparent),
                center = Offset(size.width * 0.78f, size.height * 0.15f),
                radius = size.width * 0.72f
            ),
            radius = size.width * 0.72f,
            center = Offset(size.width * 0.78f, size.height * 0.15f)
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(ElectricBlue.copy(alpha = 0.18f), Color.Transparent),
                center = Offset(size.width * 0.12f, size.height * 0.56f),
                radius = size.width * 0.65f
            ),
            radius = size.width * 0.65f,
            center = Offset(size.width * 0.12f, size.height * 0.56f)
        )
        drawLine(
            color = ElectricBlueSoft.copy(alpha = 0.18f),
            start = Offset(size.width * 0.08f, size.height * 0.05f),
            end = Offset(size.width * 0.98f, size.height * 0.98f),
            strokeWidth = 2.6.dp.toPx()
        )
    }
}

@Composable
private fun rememberCameraFrame(enabled: Boolean): Bitmap? {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var frame by remember { mutableStateOf<Bitmap?>(null) }

    DisposableEffect(enabled, lifecycleOwner) {
        if (!enabled) {
            frame = null
            return@DisposableEffect onDispose {}
        }

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val cameraExecutor = Executors.newSingleThreadExecutor()
        val mainHandler = Handler(Looper.getMainLooper())
        var analysis: ImageAnalysis? = null
        var disposed = false

        cameraProviderFuture.addListener(
            {
                if (disposed) return@addListener
                val cameraProvider = runCatching { cameraProviderFuture.get() }.getOrNull() ?: return@addListener
                val imageAnalysis = ImageAnalysis.Builder()
                    .setTargetResolution(AndroidSize(720, 1280))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                    .build()
                analysis = imageAnalysis

                var lastFrameAt = 0L
                imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    val now = SystemClock.elapsedRealtime()
                    if (now - lastFrameAt < CAMERA_FRAME_INTERVAL_MS) {
                        imageProxy.close()
                        return@setAnalyzer
                    }
                    lastFrameAt = now

                    val bitmap = runCatching { rgbaImageProxyToBitmap(imageProxy) }.getOrNull()
                    imageProxy.close()
                    if (bitmap != null) {
                        mainHandler.post {
                            if (disposed) {
                                bitmap.recycle()
                            } else {
                                frame = bitmap
                            }
                        }
                    }
                }

                runCatching {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        imageAnalysis
                    )
                }.onFailure {
                    imageAnalysis.clearAnalyzer()
                }
            },
            ContextCompat.getMainExecutor(context)
        )

        onDispose {
            disposed = true
            analysis?.clearAnalyzer()
            if (cameraProviderFuture.isDone) {
                runCatching { cameraProviderFuture.get().unbind(analysis) }
            }
            cameraExecutor.shutdown()
            frame = null
        }
    }

    return frame
}

@Composable
private fun CameraFrameBackdrop(
    frame: Bitmap?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.background(AppBlack)
    ) {
        if (frame != null) {
            Image(
                bitmap = frame.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Canvas(Modifier.fillMaxSize()) {
                drawRect(AppBlack.copy(alpha = 0.12f))
            }
        } else {
            DarkAcrylicBackground(Modifier.fillMaxSize())
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AppBlack.copy(alpha = 0.36f)),
                contentAlignment = Alignment.Center
            ) {
                RoundIconButton(
                    icon = Icons.Filled.Videocam,
                    contentDescription = null,
                    selected = true,
                    onClick = {},
                    size = 64.dp
                )
            }
        }
    }
}

@Composable
private fun CameraMagnifierLens(
    frame: Bitmap,
    zoom: Float,
    clarityMode: ClarityMode,
    lensEffects: Set<LensEffect>,
    effectIntensity: Float,
    modifier: Modifier = Modifier
) {
    val lensZoom = zoom.coerceIn(1.4f, MAX_ZOOM)
    val bitmapPaint = remember(clarityMode, lensEffects, effectIntensity) {
        enhancedBitmapPaint(clarityMode, lensEffects, effectIntensity)
    }
    Box(
        modifier = modifier
            .shadow(14.dp, CircleShape)
            .clip(CircleShape)
            .background(AppBlack)
            .border(2.dp, Rim.copy(alpha = 0.94f), CircleShape)
            .border(5.dp, ElectricBlue.copy(alpha = 0.18f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawIntoCanvas { canvas ->
                val nativeCanvas = canvas.nativeCanvas
                nativeCanvas.save()
                nativeCanvas.clipPath(android.graphics.Path().apply {
                    addCircle(
                        size.width / 2f,
                        size.height / 2f,
                        size.minDimension / 2f,
                        android.graphics.Path.Direction.CW
                    )
                })

                val source = centeredCropRect(frame.width, frame.height, 1f)
                val destinationWidth = size.width * lensZoom
                val destinationHeight = size.height * lensZoom
                nativeCanvas.drawBitmap(
                    frame,
                    source,
                    RectF(
                        (size.width - destinationWidth) / 2f,
                        (size.height - destinationHeight) / 2f,
                        (size.width + destinationWidth) / 2f,
                        (size.height + destinationHeight) / 2f
                    ),
                    bitmapPaint
                )
                nativeCanvas.restore()
            }
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color.Transparent, AppBlack.copy(alpha = 0.28f)),
                    center = center,
                    radius = size.minDimension * 0.55f
                )
            )
            drawClarityOverlay(clarityMode)
            drawCircle(
                color = Color.White.copy(alpha = 0.2f),
                radius = size.minDimension * 0.48f,
                center = center,
                style = Stroke(1.dp.toPx())
            )
        }
    }
}

@Composable
private fun ScreenCaptureMagnifierLens(
    frame: Bitmap,
    lensCenter: Offset,
    zoom: Float,
    clarityMode: ClarityMode,
    lensEffects: Set<LensEffect>,
    effectIntensity: Float,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val metrics = context.resources.displayMetrics
    val screenWidth = metrics.widthPixels.toFloat()
    val screenHeight = metrics.heightPixels.toFloat()
    val lensZoom = zoom.coerceIn(1.4f, MAX_ZOOM)
    val bitmapPaint = remember(clarityMode, lensEffects, effectIntensity) {
        enhancedBitmapPaint(clarityMode, lensEffects, effectIntensity)
    }

    Box(
        modifier = modifier
            .shadow(14.dp, CircleShape)
            .clip(CircleShape)
            .background(AppBlack)
            .border(2.dp, Rim.copy(alpha = 0.94f), CircleShape)
            .border(5.dp, ElectricBlue.copy(alpha = 0.18f), CircleShape)
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawIntoCanvas { canvas ->
                val nativeCanvas = canvas.nativeCanvas
                nativeCanvas.save()
                nativeCanvas.clipPath(android.graphics.Path().apply {
                    addCircle(size.width / 2f, size.height / 2f, size.minDimension / 2f, android.graphics.Path.Direction.CW)
                })
                nativeCanvas.translate(
                    size.width / 2f - lensCenter.x * lensZoom,
                    size.height / 2f - lensCenter.y * lensZoom
                )
                nativeCanvas.scale(lensZoom, lensZoom)
                nativeCanvas.drawBitmap(
                    frame,
                    null,
                    RectF(0f, 0f, screenWidth, screenHeight),
                    bitmapPaint
                )
                nativeCanvas.restore()
            }

            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color.Transparent, AppBlack.copy(alpha = 0.28f)),
                    center = center,
                    radius = size.minDimension * 0.55f
                )
            )
            drawClarityOverlay(clarityMode)
            drawCircle(
                color = Color.White.copy(alpha = 0.2f),
                radius = size.minDimension * 0.48f,
                center = center,
                style = Stroke(1.dp.toPx())
            )
        }
    }
}

private fun enhancedBitmapPaint(
    mode: ClarityMode,
    effects: Set<LensEffect>,
    intensity: Float
): Paint {
    val colorMatrix = lensColorMatrix(mode, effects, intensity.coerceIn(0f, 1f))
    return Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG).apply {
        isFilterBitmap = true
        isDither = true
        colorFilter = colorMatrix?.let(::ColorMatrixColorFilter)
    }
}

private fun lensColorMatrix(
    mode: ClarityMode,
    effects: Set<LensEffect>,
    intensity: Float
): ColorMatrix? {
    val result = ColorMatrix()
    var changed = false

    StackableLensEffects.forEach { effect ->
        if (effect in effects) {
            effectColorMatrix(effect, intensity)?.let {
                result.postConcat(it)
                changed = true
            }
        }
    }

    clarityColorMatrix(mode)?.let {
        result.postConcat(it)
        changed = true
    }

    return if (changed) result else null
}

private fun effectColorMatrix(effect: LensEffect, intensity: Float): ColorMatrix? {
    val amount = intensity.coerceIn(0f, 1f)
    return when (effect) {
        LensEffect.Standard -> null

        LensEffect.TextBoost -> {
            val monochrome = ColorMatrix().apply {
                setSaturation((0.08f * (1f - amount)).coerceIn(0f, 1f))
            }
            val contrast = contrastSaturationMatrix(
                contrast = 1f + 0.95f * amount,
                saturation = 0.1f
            )
            monochrome.postConcat(contrast)
            monochrome
        }

        LensEffect.EdgeDetail -> contrastSaturationMatrix(
            contrast = 1f + 0.38f * amount,
            saturation = 1f + 0.04f * amount
        )

        LensEffect.LowLight -> brightnessContrastMatrix(
            contrast = 1f + 0.16f * amount,
            brightness = 36f * amount,
            saturation = 1f + 0.06f * amount
        )

        LensEffect.ColorDetail -> contrastSaturationMatrix(
            contrast = 1f + 0.18f * amount,
            saturation = 1f + 0.46f * amount
        )

        LensEffect.Monochrome -> ColorMatrix().apply {
            setSaturation((1f - amount).coerceIn(0f, 1f))
        }

        LensEffect.Invert -> {
            val scale = 1f - 2f * amount
            val translate = 255f * amount
            ColorMatrix(
                floatArrayOf(
                    scale, 0f, 0f, 0f, translate,
                    0f, scale, 0f, 0f, translate,
                    0f, 0f, scale, 0f, translate,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        }

        LensEffect.HighContrast -> contrastSaturationMatrix(
            contrast = 1f + 0.65f * amount,
            saturation = 1f + 0.1f * amount
        )
    }
}

private fun clarityColorMatrix(mode: ClarityMode): ColorMatrix? {
    return when (mode) {
        ClarityMode.Off -> null
        ClarityMode.Fast -> contrastSaturationMatrix(contrast = 1.07f, saturation = 1.04f)
        ClarityMode.Sharp -> contrastSaturationMatrix(contrast = 1.16f, saturation = 1.07f)
    }
}

private fun contrastSaturationMatrix(contrast: Float, saturation: Float): ColorMatrix {
    val translate = (-0.5f * contrast + 0.5f) * 255f
    val contrastMatrix = ColorMatrix(
        floatArrayOf(
            contrast, 0f, 0f, 0f, translate,
            0f, contrast, 0f, 0f, translate,
            0f, 0f, contrast, 0f, translate,
            0f, 0f, 0f, 1f, 0f
        )
    )
    val saturationMatrix = ColorMatrix().apply { setSaturation(saturation) }
    saturationMatrix.postConcat(contrastMatrix)
    return saturationMatrix
}

private fun brightnessContrastMatrix(
    contrast: Float,
    brightness: Float,
    saturation: Float
): ColorMatrix {
    val translate = (-0.5f * contrast + 0.5f) * 255f + brightness
    val contrastMatrix = ColorMatrix(
        floatArrayOf(
            contrast, 0f, 0f, 0f, translate,
            0f, contrast, 0f, 0f, translate,
            0f, 0f, contrast, 0f, translate,
            0f, 0f, 0f, 1f, 0f
        )
    )
    val saturationMatrix = ColorMatrix().apply { setSaturation(saturation) }
    saturationMatrix.postConcat(contrastMatrix)
    return saturationMatrix
}

private fun centeredCropRect(width: Int, height: Int, targetAspect: Float): Rect {
    val sourceAspect = width.toFloat() / height.toFloat()
    return if (sourceAspect > targetAspect) {
        val cropWidth = (height * targetAspect).roundToInt().coerceIn(1, width)
        val left = (width - cropWidth) / 2
        Rect(left, 0, left + cropWidth, height)
    } else {
        val cropHeight = (width / targetAspect).roundToInt().coerceIn(1, height)
        val top = (height - cropHeight) / 2
        Rect(0, top, width, top + cropHeight)
    }
}

private fun DrawScope.drawClarityOverlay(mode: ClarityMode) {
    if (mode == ClarityMode.Off) return

    val glowAlpha = if (mode == ClarityMode.Sharp) 0.18f else 0.1f
    val strokeAlpha = if (mode == ClarityMode.Sharp) 0.42f else 0.28f
    drawCircle(
        color = ElectricBlue.copy(alpha = glowAlpha),
        radius = size.minDimension * 0.47f,
        center = center,
        style = Stroke(if (mode == ClarityMode.Sharp) 2.dp.toPx() else 1.dp.toPx())
    )
    drawCircle(
        color = Color.White.copy(alpha = strokeAlpha),
        radius = size.minDimension * 0.33f,
        center = center,
        style = Stroke(0.8.dp.toPx())
    )
}

private fun rgbaImageProxyToBitmap(imageProxy: ImageProxy): Bitmap {
    val plane = imageProxy.planes[0]
    val width = imageProxy.width
    val height = imageProxy.height
    val rowStride = plane.rowStride
    val pixelStride = plane.pixelStride
    val buffer = plane.buffer.apply { rewind() }

    val bitmap = if (rowStride == width * pixelStride) {
        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also {
            it.copyPixelsFromBuffer(buffer)
        }
    } else {
        val paddedWidth = rowStride / pixelStride
        val paddedBitmap = Bitmap.createBitmap(paddedWidth, height, Bitmap.Config.ARGB_8888).also {
            it.copyPixelsFromBuffer(buffer)
        }
        Bitmap.createBitmap(paddedBitmap, 0, 0, width, height).also {
            paddedBitmap.recycle()
        }
    }

    val rotation = imageProxy.imageInfo.rotationDegrees
    if (rotation == 0) return bitmap

    val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true).also {
        if (it != bitmap) bitmap.recycle()
    }
}

@Composable
private fun MagnifierPage(
    activeEffects: Set<LensEffect>,
    onEffectSelected: (LensEffect) -> Unit,
    zoom: Float,
    onZoomChange: (Float) -> Unit,
    intensity: Float,
    onIntensityChange: (Float) -> Unit,
    clarityMode: ClarityMode,
    onClarityModeChange: (ClarityMode) -> Unit,
    onStartOverlay: () -> Unit,
    overlayActive: Boolean,
    cameraActive: Boolean,
    cameraFrame: Bitmap?,
    onCaptureToggle: () -> Unit,
    onHistory: () -> Unit,
    onSettings: () -> Unit
) {
    var menuVisible by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            EffectWheel(
                activeEffects = activeEffects,
                onEffectSelected = onEffectSelected,
                enabled = !overlayActive,
                menuToggleEnabled = true,
                menuVisible = menuVisible,
                onLensDoubleTap = { menuVisible = !menuVisible },
                lensFrame = if (cameraActive) cameraFrame else null,
                lensZoom = zoom.coerceIn(1.4f, MAX_ZOOM),
                clarityMode = clarityMode,
                effectIntensity = intensity,
                includeCaptureAction = true,
                includeEnhanceAction = true,
                captureActive = cameraActive,
                onCaptureClick = onCaptureToggle,
                onEnhanceClick = { onClarityModeChange(clarityMode.next()) },
                modifier = Modifier.fillMaxSize()
            )
            QuickPageButtons(
                onHistory = onHistory,
                onSettings = onSettings,
                modifier = Modifier.align(Alignment.TopEnd)
            )
            if (overlayActive) {
                OverlayActiveBadge(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 4.dp)
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GlassSliderCard(
                title = "Zoom",
                valueLabel = String.format(Locale.US, "%.1fx", zoom),
                value = zoom,
                valueRange = MIN_ZOOM..MAX_ZOOM,
                onValueChange = onZoomChange,
                onMinus = { onZoomChange((zoom - 0.1f).coerceAtLeast(MIN_ZOOM)) },
                onPlus = { onZoomChange((zoom + 0.1f).coerceAtMost(MAX_ZOOM)) },
                enabled = !overlayActive,
                modifier = Modifier.weight(1f)
            )
            GlassSliderCard(
                title = "Intensity",
                valueLabel = "${(intensity * 100).toInt()}%",
                value = intensity,
                valueRange = 0f..1f,
                onValueChange = onIntensityChange,
                onMinus = { onIntensityChange((intensity - 0.05f).coerceAtLeast(0f)) },
                onPlus = { onIntensityChange((intensity + 0.05f).coerceAtMost(1f)) },
                enabled = !overlayActive,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SquareActionButton(
                    label = if (overlayActive) "Overlay On" else "Overlay",
                    icon = Icons.Filled.CenterFocusStrong,
                    selected = overlayActive,
                    onClick = onStartOverlay,
                    enabled = !overlayActive,
                    modifier = Modifier.size(76.dp)
                )
                SquareActionButton(
                    label = "Capture",
                    icon = if (cameraActive) Icons.Filled.Videocam else Icons.Filled.PhotoCamera,
                    selected = cameraActive,
                    onClick = onCaptureToggle,
                    enabled = !overlayActive,
                    modifier = Modifier.size(76.dp)
                )
                SquareActionButton(
                    label = clarityMode.buttonLabel,
                    icon = clarityMode.icon,
                    selected = clarityMode != ClarityMode.Off,
                    onClick = { onClarityModeChange(clarityMode.next()) },
                    enabled = !overlayActive,
                    modifier = Modifier.size(76.dp)
                )
            }
        }
    }
}

@Composable
private fun QuickPageButtons(
    onHistory: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        RoundIconButton(
            icon = Icons.Filled.History,
            contentDescription = "Open history",
            selected = false,
            onClick = onHistory,
            size = 42.dp
        )
        RoundIconButton(
            icon = Icons.Filled.Settings,
            contentDescription = "Open settings",
            selected = false,
            onClick = onSettings,
            size = 42.dp
        )
    }
}

@Composable
private fun OverlayActiveBadge(modifier: Modifier = Modifier) {
    GlassPanel(
        modifier = modifier
            .height(42.dp)
            .widthIn(min = 176.dp),
        shape = RoundedCornerShape(18.dp),
        selected = true
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.CenterFocusStrong,
                contentDescription = null,
                tint = ElectricBlueSoft,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(7.dp))
            Text(
                text = "Overlay active",
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun EffectWheel(
    activeEffects: Set<LensEffect>,
    onEffectSelected: (LensEffect) -> Unit,
    enabled: Boolean = true,
    menuToggleEnabled: Boolean = false,
    menuVisible: Boolean = true,
    onLensDrag: ((Int, Int) -> Unit)? = null,
    onLensDoubleTap: (() -> Unit)? = null,
    lensFrame: Bitmap? = null,
    screenLensCenter: Offset? = null,
    lensZoom: Float = 2f,
    clarityMode: ClarityMode = ClarityMode.Off,
    effectIntensity: Float = 0.75f,
    includeCaptureAction: Boolean = false,
    includeEnhanceAction: Boolean = false,
    captureActive: Boolean = false,
    onCaptureClick: (() -> Unit)? = null,
    onEnhanceClick: (() -> Unit)? = null,
    controlSize: Dp? = null,
    lensSize: Dp? = null,
    smallGuideText: Boolean = false,
    modifier: Modifier = Modifier
) {
    var dialRotationDegrees by remember { mutableStateOf(0f) }
    val dialItems = remember(includeCaptureAction, includeEnhanceAction) {
        buildMagnifierDialItems(includeCaptureAction, includeEnhanceAction)
    }
    val menuProgress by animateFloatAsState(
        targetValue = if (!menuToggleEnabled || menuVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 220),
        label = "wheelMenuProgress"
    )
    var guideItem by remember(dialItems) { mutableStateOf<DialItem?>(null) }
    var guideVisible by remember { mutableStateOf(false) }
    var guideNonce by remember { mutableStateOf(0) }
    fun showGuideForRotation(rotation: Float) {
        guideItem = pointedDialItem(dialItems, rotation)
        guideVisible = guideItem != null
        guideNonce += 1
    }
    fun showGuideForItem(item: DialItem) {
        guideItem = item
        guideVisible = true
        guideNonce += 1
    }
    LaunchedEffect(guideNonce) {
        if (guideNonce > 0) {
            delay(3000)
            guideVisible = false
        }
    }
    val guideProgress by animateFloatAsState(
        targetValue = if ((!menuToggleEnabled || menuVisible) && guideVisible && guideItem != null) 1f else 0f,
        animationSpec = tween(durationMillis = 180),
        label = "dialGuideProgress"
    )
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = modifier
            .semantics {
                contentDescription = "Circular magnifier effect wheel"
            },
        contentAlignment = Alignment.Center
    ) {
        val side = min(maxWidth.value, maxHeight.value).dp
        val targetControlSize = controlSize ?: side
        val scale = (targetControlSize / 344.dp).coerceAtMost(1f)
        val bodySize = controlSize ?: (344.dp * scale)
        val longOffset = 122.dp * scale
        val nodeTouchSize = if (smallGuideText) {
            (54.dp * scale).coerceAtLeast(44.dp)
        } else {
            (54.dp * scale).coerceAtLeast(36.dp)
        }
        val nodeButtonSize = (48.dp * scale).coerceAtLeast(32.dp)
        val defaultLensHotspotSize = if (menuToggleEnabled && !menuVisible) {
            bodySize
        } else {
            (bodySize * 0.48f).coerceAtLeast(70.dp)
        }
        val lensHotspotSize = lensSize ?: defaultLensHotspotSize
        val contentSize = if (lensHotspotSize > bodySize) lensHotspotSize else bodySize
        val bodySizePx = with(density) { bodySize.toPx() }
        val wheelGestureModifier = if (enabled && (!menuToggleEnabled || menuVisible)) {
            Modifier.pointerInput(bodySizePx, dialItems) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val distanceFromCenter = (down.position - center).getDistance()
                    val innerDialRadius = bodySizePx * if (smallGuideText) 0.20f else 0.24f
                    val outerDialRadius = bodySizePx * if (smallGuideText) 0.64f else 0.54f
                    if (distanceFromCenter !in innerDialRadius..outerDialRadius) {
                        return@awaitEachGesture
                    }

                    var dragRotation = dialRotationDegrees
                    showGuideForRotation(dragRotation)
                    var previousAngle = angleDegreesFromCenter(down.position, center)
                    drag(down.id) { change ->
                        val nextAngle = angleDegreesFromCenter(change.position, center)
                        val delta = shortestAngleDeltaDegrees(previousAngle, nextAngle)
                        dragRotation = normalizeAngleDegrees(dragRotation + delta)
                        dialRotationDegrees = dragRotation
                        showGuideForRotation(dragRotation)
                        previousAngle = nextAngle
                        change.consume()
                    }
                }
            }
        } else {
            Modifier
        }

        Box(
            modifier = Modifier
                .size(contentSize)
                .then(wheelGestureModifier),
            contentAlignment = Alignment.Center
        ) {
            WheelBody(
                menuProgress = menuProgress,
                rotationDegrees = dialRotationDegrees,
                drawLensFill = lensFrame == null,
                controlDiameter = bodySize,
                lensDiameter = lensSize,
                modifier = Modifier.size(contentSize)
            )

            if (lensFrame != null) {
                if (screenLensCenter != null) {
                    ScreenCaptureMagnifierLens(
                        frame = lensFrame,
                        lensCenter = screenLensCenter,
                        zoom = lensZoom,
                        clarityMode = clarityMode,
                        lensEffects = activeEffects,
                        effectIntensity = effectIntensity,
                        modifier = Modifier.size(lensHotspotSize)
                    )
                } else {
                    CameraMagnifierLens(
                        frame = lensFrame,
                        zoom = lensZoom,
                        clarityMode = clarityMode,
                        lensEffects = activeEffects,
                        effectIntensity = effectIntensity,
                        modifier = Modifier.size(lensHotspotSize)
                    )
            }
        }

            if (enabled && (menuToggleEnabled || onLensDrag != null || onLensDoubleTap != null)) {
                var lensModifier = Modifier
                    .size(lensHotspotSize)
                    .clip(CircleShape)
                    .semantics {
                        contentDescription = "Double tap lens to show or hide effect menu"
                    }
                if (onLensDoubleTap != null) {
                    lensModifier = lensModifier.pointerInput(onLensDoubleTap) {
                        detectTapGestures(onDoubleTap = { onLensDoubleTap() })
                    }
                }
                if (onLensDrag != null) {
                    lensModifier = lensModifier.pointerInput(onLensDrag) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            onLensDrag(dragAmount.x.roundToInt(), dragAmount.y.roundToInt())
                        }
                    }
                }
                Box(
                    modifier = lensModifier
                )
            }

            dialItems.forEach { item ->
                val angleRadians = Math.toRadians((item.baseAngleDegrees + dialRotationDegrees).toDouble())
                val xOffset = (cos(angleRadians).toFloat() * longOffset.value).dp
                val yOffset = (sin(angleRadians).toFloat() * longOffset.value).dp
                val effect = item.effect
                if (effect != null) {
                    LensEffectNode(
                        effect = effect,
                        selected = if (effect == LensEffect.Standard) activeEffects.isEmpty() else effect in activeEffects,
                        enabled = enabled && (!menuToggleEnabled || menuVisible),
                        menuProgress = menuProgress,
                        onClick = {
                            showGuideForItem(item)
                            onEffectSelected(effect)
                        },
                        nodeSize = nodeTouchSize,
                        buttonSize = nodeButtonSize,
                        modifier = Modifier.offset(x = xOffset, y = yOffset)
                    )
                } else {
                    val action = item.action
                    val isClarityAction = action == DialAction.Clarity
                    val actionEnabled = if (isClarityAction) {
                        onEnhanceClick != null
                    } else {
                        onCaptureClick != null
                    }
                    DialActionNode(
                        label = if (isClarityAction) "Clarity ${clarityMode.label}" else item.label,
                        icon = if (isClarityAction) clarityMode.icon else item.icon,
                        selected = if (isClarityAction) clarityMode != ClarityMode.Off else captureActive,
                        enabled = enabled && (!menuToggleEnabled || menuVisible) && actionEnabled,
                        menuProgress = menuProgress,
                        onClick = {
                            showGuideForItem(item)
                            when (action) {
                                DialAction.Clarity -> onEnhanceClick?.invoke()
                                DialAction.Capture -> onCaptureClick?.invoke()
                                null -> Unit
                            }
                        },
                        nodeSize = nodeTouchSize,
                        buttonSize = nodeButtonSize,
                        modifier = Modifier.offset(x = xOffset, y = yOffset)
                    )
                }
            }

            guideItem?.let { item ->
                val compactGuide = smallGuideText || lensHotspotSize < 118.dp
                DialGuideCallout(
                    guideText = dialGuideText(item, clarityMode, captureActive),
                    progress = guideProgress,
                    compact = compactGuide,
                    modifier = Modifier.size(lensHotspotSize)
                )
            }
        }
    }
}

@Composable
private fun DialGuideCallout(
    guideText: DialGuideText,
    progress: Float,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    if (progress <= 0.01f) return

    val titleFont = if (compact) 9.sp else 15.sp
    val descriptionFont = if (compact) 6.5.sp else 11.sp
    val iconSize = if (compact) 10.dp else 17.dp
    val maxWidth = if (compact) 88.dp else 184.dp
    val horizontalPadding = if (compact) 5.dp else 12.dp
    val verticalPadding = if (compact) 3.dp else 8.dp

    Box(
        modifier = modifier
            .graphicsLayer {
                alpha = progress
                val calloutScale = 0.94f + (0.06f * progress)
                scaleX = calloutScale
                scaleY = calloutScale
            }
            .semantics {
                contentDescription = "${guideText.title}. ${guideText.description}"
            },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(if (compact) 10.dp else 16.dp),
            color = if (compact) Color(0xFF071522).copy(alpha = 0.96f) else AppBlack.copy(alpha = 0.84f),
            border = BorderStroke(1.dp, ElectricBlue.copy(alpha = if (compact) 0.88f else 0.52f))
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = maxWidth)
                    .padding(horizontal = horizontalPadding, vertical = verticalPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(if (compact) 1.dp else 2.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(if (compact) 3.dp else 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = guideText.icon,
                        contentDescription = null,
                        tint = ElectricBlueSoft,
                        modifier = Modifier.size(iconSize)
                    )
                    Text(
                        text = guideText.title,
                        color = TextPrimary,
                        fontSize = titleFont,
                        lineHeight = titleFont,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = guideText.description,
                    color = TextSecondary,
                    fontSize = descriptionFont,
                    lineHeight = descriptionFont * 1.16f,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = if (compact) 2 else 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun WheelBody(
    menuProgress: Float = 1f,
    rotationDegrees: Float = 0f,
    drawLensFill: Boolean = true,
    controlDiameter: Dp? = null,
    lensDiameter: Dp? = null,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val controlDimension = controlDiameter?.toPx()
            ?.coerceAtMost(size.minDimension)
            ?: size.minDimension
        val outer = controlDimension * 0.42f
        val inner = outer * 0.56f
        val handleRadius = outer * 0.19f
        val handleCenter = Offset(center.x, center.y + outer * 0.86f)
        val menuAlpha = menuProgress.coerceIn(0f, 1f)

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(ElectricBlue.copy(alpha = 0.26f * menuAlpha), Color.Transparent),
                center = center,
                radius = outer * 1.38f
            ),
            radius = outer * 1.38f,
            center = center
        )
        drawCircle(
            color = AppPanel.copy(alpha = AppPanel.alpha * menuAlpha),
            radius = outer,
            center = center
        )
        drawCircle(
            color = AppPanel.copy(alpha = AppPanel.alpha * menuAlpha),
            radius = handleRadius,
            center = handleCenter
        )
        drawRoundRect(
            color = AppPanel.copy(alpha = AppPanel.alpha * menuAlpha),
            topLeft = Offset(center.x - handleRadius, center.y + outer * 0.55f),
            size = Size(handleRadius * 2f, handleRadius * 1.25f),
            cornerRadius = CornerRadius(handleRadius)
        )
        if (drawLensFill) {
            val defaultHiddenLensRadius = controlDimension * 0.48f
            val lensRadius = lensDiameter?.toPx()?.let { it / 2f }
                ?: (defaultHiddenLensRadius + (inner - defaultHiddenLensRadius) * menuAlpha)
            drawCircle(
                color = AppBlack.copy(alpha = 0.9f),
                radius = lensRadius,
                center = center
            )
        }
        val rotationRadians = Math.toRadians(rotationDegrees.toDouble())
        repeat(8) { index ->
            val angle = -PI / 2 + index * PI / 4 + rotationRadians
            drawLine(
                color = Rim.copy(alpha = 0.18f * menuAlpha),
                start = Offset(
                    center.x + cos(angle).toFloat() * (inner + 8.dp.toPx()),
                    center.y + sin(angle).toFloat() * (inner + 8.dp.toPx())
                ),
                end = Offset(
                    center.x + cos(angle).toFloat() * (outer - 8.dp.toPx()),
                    center.y + sin(angle).toFloat() * (outer - 8.dp.toPx())
                ),
                strokeWidth = 1.dp.toPx()
            )
        }
        drawCircle(Rim.copy(alpha = 0.32f * menuAlpha), outer, center, style = Stroke(2.dp.toPx()))
        drawCircle(Rim.copy(alpha = 0.82f * menuAlpha), inner + 7.dp.toPx(), center, style = Stroke(2.dp.toPx()))
        drawCircle(Color.White.copy(alpha = 0.56f * menuAlpha), inner + 13.dp.toPx(), center, style = Stroke(1.dp.toPx()))
        drawCircle(ElectricBlue.copy(alpha = 0.2f * menuAlpha), outer + 2.dp.toPx(), center, style = Stroke(1.dp.toPx()))

        val arrowStrokeWidth = 3.dp.toPx()
        drawLine(
            color = Color.White.copy(alpha = 0.9f * menuAlpha),
            start = Offset(center.x - 7.dp.toPx(), center.y - inner - 24.dp.toPx()),
            end = Offset(center.x, center.y - inner - 32.dp.toPx()),
            strokeWidth = arrowStrokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color.White.copy(alpha = 0.9f * menuAlpha),
            start = Offset(center.x + 7.dp.toPx(), center.y - inner - 24.dp.toPx()),
            end = Offset(center.x, center.y - inner - 32.dp.toPx()),
            strokeWidth = arrowStrokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color.White.copy(alpha = 0.9f * menuAlpha),
            start = Offset(center.x - 7.dp.toPx(), handleCenter.y + handleRadius * 0.25f),
            end = Offset(center.x, handleCenter.y + handleRadius * 0.12f),
            strokeWidth = arrowStrokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color.White.copy(alpha = 0.9f * menuAlpha),
            start = Offset(center.x + 7.dp.toPx(), handleCenter.y + handleRadius * 0.25f),
            end = Offset(center.x, handleCenter.y + handleRadius * 0.12f),
            strokeWidth = arrowStrokeWidth,
            cap = StrokeCap.Round
        )
    }
}

private fun angleDegreesFromCenter(position: Offset, center: Offset): Float {
    return Math.toDegrees(
        atan2(
            y = (position.y - center.y).toDouble(),
            x = (position.x - center.x).toDouble()
        )
    ).toFloat()
}

private fun shortestAngleDeltaDegrees(from: Float, to: Float): Float {
    var delta = (to - from) % 360f
    if (delta > 180f) delta -= 360f
    if (delta < -180f) delta += 360f
    return delta
}

private fun normalizeAngleDegrees(angle: Float): Float {
    val normalized = angle % 360f
    return if (normalized < 0f) normalized + 360f else normalized
}

@Composable
private fun LensEffectNode(
    effect: LensEffect,
    selected: Boolean,
    enabled: Boolean = true,
    menuProgress: Float = 1f,
    onClick: () -> Unit,
    nodeSize: Dp = 54.dp,
    buttonSize: Dp = 48.dp,
    modifier: Modifier = Modifier
) {
    val nodeAlpha = menuProgress.coerceIn(0f, 1f)
    val nodeScale = 0.82f + (0.18f * nodeAlpha)

    Box(
        modifier = modifier
            .size(nodeSize)
            .graphicsLayer {
                alpha = nodeAlpha
                scaleX = nodeScale
                scaleY = nodeScale
            }
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics {
                contentDescription = when {
                    !enabled -> "${effect.label} hidden"
                    effect == LensEffect.Standard -> "Clear effects"
                    selected -> "Disable ${effect.label}"
                    else -> "Enable ${effect.label}"
                }
            },
        contentAlignment = Alignment.Center
    ) {
        RoundIconButton(
            icon = effect.icon,
            contentDescription = null,
            selected = selected,
            onClick = onClick,
            size = buttonSize,
            iconTint = TextPrimary,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
private fun DialActionNode(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    enabled: Boolean = true,
    menuProgress: Float = 1f,
    onClick: () -> Unit,
    nodeSize: Dp = 54.dp,
    buttonSize: Dp = 48.dp,
    modifier: Modifier = Modifier
) {
    val nodeAlpha = menuProgress.coerceIn(0f, 1f)
    val nodeScale = 0.82f + (0.18f * nodeAlpha)

    Box(
        modifier = modifier
            .size(nodeSize)
            .graphicsLayer {
                alpha = nodeAlpha
                scaleX = nodeScale
                scaleY = nodeScale
            }
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics {
                contentDescription = if (enabled) label else "$label unavailable"
            },
        contentAlignment = Alignment.Center
    ) {
        RoundIconButton(
            icon = icon,
            contentDescription = null,
            selected = selected,
            enabled = enabled,
            onClick = onClick,
            size = buttonSize,
            iconTint = if (selected) ElectricBlueSoft else TextPrimary,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
private fun GlassSliderCard(
    title: String,
    valueLabel: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    GlassPanel(
        modifier = modifier
            .height(84.dp)
            .graphicsLayer { alpha = if (enabled) 1f else 0.48f },
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = valueLabel,
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SmallRoundControl(Icons.Filled.Remove, "Decrease $title", onMinus, enabled = enabled)
                Slider(
                    value = value,
                    onValueChange = { if (enabled) onValueChange(it) },
                    valueRange = valueRange,
                    enabled = enabled,
                    colors = SliderDefaults.colors(
                        thumbColor = TextPrimary,
                        activeTrackColor = ElectricBlue,
                        inactiveTrackColor = Color(0xFF26313D)
                    ),
                    modifier = Modifier.weight(1f)
                )
                SmallRoundControl(Icons.Filled.Add, "Increase $title", onPlus, enabled = enabled)
            }
        }
    }
}

@Composable
private fun SmallRoundControl(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    size: Dp = 38.dp
) {
    RoundIconButton(
        icon = icon,
        contentDescription = contentDescription,
        selected = false,
        onClick = onClick,
        enabled = enabled,
        size = size
    )
}

@Composable
private fun SquareActionButton(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier) {
        val compact = maxWidth < 78.dp
        val iconSize = if (compact) 24.dp else 32.dp
        val labelSize = if (compact) 10.sp else 12.sp
        val verticalGap = if (compact) 4.dp else 6.dp
        val padding = if (compact) 6.dp else 8.dp

        GlassPanel(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = if (enabled) 1f else 0.5f }
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
                .semantics { contentDescription = label },
            shape = RoundedCornerShape(if (compact) 18.dp else 20.dp),
            selected = selected
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (selected) ElectricBlueSoft else TextPrimary,
                    modifier = Modifier.size(iconSize)
                )
                Spacer(Modifier.height(verticalGap))
                Text(
                    text = label,
                    color = if (selected) ElectricBlueSoft else TextPrimary,
                    fontSize = labelSize,
                    lineHeight = (labelSize.value + 2f).sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun HistoryPage(savedLens: List<SavedLens>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PageHeader(
                title = "History",
                subtitle = "Recent captures and lens presets"
            )
            RoundIconButton(
                icon = Icons.Filled.FilterList,
                contentDescription = "Filter history",
                selected = false,
                onClick = {},
                size = 48.dp
            )
        }

        savedLens.forEach { item ->
            HistoryCard(item = item)
        }
    }
}

@Composable
private fun HistoryCard(item: SavedLens) {
    GlassPanel(
        modifier = Modifier
            .fillMaxWidth()
            .height(112.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LensPreview(
                kind = item.preview,
                modifier = Modifier.size(72.dp)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.title,
                        color = TextPrimary,
                        fontSize = 17.sp,
                        lineHeight = 19.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Icon(
                        imageVector = Icons.Filled.MoreHoriz,
                        contentDescription = "More options",
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                EffectStackChips(effects = item.effects)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MetaText(Icons.Filled.Search, String.format(Locale.US, "%.1fx", item.zoom))
                    MetaText(Icons.Filled.Brightness5, "${item.intensity}%")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.CalendarToday,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = item.date,
                        color = TextSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = item.time,
                        color = TextMuted,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            RestoreButton()
        }
    }
}

@Composable
private fun LensPreview(kind: PreviewKind, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .shadow(8.dp, CircleShape)
            .clip(CircleShape)
            .background(Color(0xFFEDF3F7))
            .border(3.dp, Rim.copy(alpha = 0.8f), CircleShape)
            .border(1.dp, Color.White.copy(alpha = 0.92f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        when (kind) {
            PreviewKind.Reading -> PreviewReading()
            PreviewKind.Receipt -> PreviewReceipt()
            PreviewKind.Map -> PreviewMap()
            PreviewKind.Article -> PreviewArticle()
            PreviewKind.Photo -> PreviewPhoto()
        }
    }
}

@Composable
private fun PreviewReading() {
    Column(
        modifier = Modifier.padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = "Mountain",
            color = Color(0xFF5D45B7),
            fontSize = 7.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        repeat(4) {
            Box(
                Modifier
                    .height(1.5.dp)
                    .fillMaxWidth(if (it == 3) 0.74f else 1f)
                    .background(Color(0xFF1C2530).copy(alpha = 0.7f), RoundedCornerShape(2.dp))
            )
        }
    }
}

@Composable
private fun PreviewReceipt() {
    Column(
        modifier = Modifier.padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Total", color = Color(0xFF1A1E25), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Text("$23.48", color = Color(0xFF1A1E25), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
        Box(Modifier.height(1.dp).fillMaxWidth().background(Color(0x771A1E25)))
        Text("Thank you!", color = Color(0xFF1A1E25), fontSize = 6.sp)
    }
}

@Composable
private fun PreviewMap() {
    Canvas(Modifier.fillMaxSize()) {
        drawRect(Color(0xFFE9F0E6))
        drawLine(Color(0xFFCFA76A), Offset(size.width * 0.1f, size.height), Offset(size.width * 0.9f, 0f), 5.dp.toPx())
        drawLine(Color(0xFFB6C8D8), Offset(0f, size.height * 0.35f), Offset(size.width, size.height * 0.2f), 4.dp.toPx())
        drawLine(Color(0xFFB6C8D8), Offset(size.width * 0.25f, 0f), Offset(size.width * 0.55f, size.height), 4.dp.toPx())
        drawCircle(ElectricBlue, size.minDimension * 0.12f, Offset(size.width * 0.68f, size.height * 0.48f))
        drawCircle(Color.White, size.minDimension * 0.04f, Offset(size.width * 0.68f, size.height * 0.48f))
    }
}

@Composable
private fun PreviewArticle() {
    Column(
        modifier = Modifier.padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text = "The Future\nof Design",
            color = Color(0xFF18202A),
            fontSize = 9.sp,
            lineHeight = 10.sp,
            fontWeight = FontWeight.ExtraBold
        )
        repeat(3) {
            Box(
                Modifier
                    .height(1.5.dp)
                    .fillMaxWidth(if (it == 2) 0.68f else 1f)
                    .background(Color(0xFF1C2530).copy(alpha = 0.55f), RoundedCornerShape(2.dp))
            )
        }
    }
}

@Composable
private fun PreviewPhoto() {
    Canvas(Modifier.fillMaxSize()) {
        drawRect(Color(0xFFB58A58))
        drawCircle(Color(0xFF6D4A2A).copy(alpha = 0.55f), size.minDimension * 0.42f, Offset(size.width * 0.62f, size.height * 0.56f))
        val dome = Path().apply {
            moveTo(size.width * 0.15f, size.height * 0.7f)
            cubicTo(size.width * 0.22f, size.height * 0.3f, size.width * 0.78f, size.height * 0.3f, size.width * 0.85f, size.height * 0.7f)
            close()
        }
        drawPath(dome, Color(0xFF3E2B1D).copy(alpha = 0.6f))
        drawRect(Color(0xFF2F2117).copy(alpha = 0.72f), Offset(size.width * 0.18f, size.height * 0.62f), Size(size.width * 0.64f, size.height * 0.2f))
    }
}

@Composable
private fun EffectStackChips(effects: Set<LensEffect>, modifier: Modifier = Modifier) {
    val active = StackableLensEffects.filter { it in effects }
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (active.isEmpty()) {
            EffectChip(effect = LensEffect.Standard)
        } else {
            active.take(2).forEach { effect ->
                EffectChip(effect = effect)
            }
            if (active.size > 2) {
                EffectCountChip(count = active.size - 2)
            }
        }
    }
}

@Composable
private fun EffectChip(effect: LensEffect, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(40.dp),
        color = Color(0xC4070B10),
        border = BorderStroke(1.dp, RimDim.copy(alpha = 0.72f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = effect.icon,
                contentDescription = null,
                tint = TextPrimary,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = effect.label,
                color = TextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun EffectCountChip(count: Int, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(40.dp),
        color = ElectricBlue.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.48f))
    ) {
        Text(
            text = "+$count",
            color = TextPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun MetaText(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = ElectricBlue,
            modifier = Modifier.size(15.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = text,
            color = TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun RestoreButton() {
    Surface(
        modifier = Modifier
            .width(76.dp)
            .height(40.dp)
            .clickable(role = Role.Button, onClick = {}),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xCC080C11),
        border = BorderStroke(1.dp, Rim.copy(alpha = 0.56f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Restore,
                contentDescription = null,
                tint = TextPrimary,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(3.dp))
            Text(
                text = "Restore",
                color = TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SettingsPage(
    activeEffects: Set<LensEffect>,
    onEffectSelected: (LensEffect) -> Unit,
    zoom: Float,
    onZoomChange: (Float) -> Unit,
    intensity: Float,
    onIntensityChange: (Float) -> Unit,
    clarityMode: ClarityMode,
    onClarityModeChange: (ClarityMode) -> Unit
) {
    var floatingControls by remember { mutableStateOf(true) }
    var circularLens by remember { mutableStateOf(true) }
    var saveHistory by remember { mutableStateOf(true) }
    var edgeGlow by remember { mutableStateOf(true) }
    var showCapture by remember { mutableStateOf(true) }
    var highContrastLabels by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        PageHeader(
            title = "Settings",
            subtitle = "Overlay behavior and lens effects"
        )

        SettingsGroup(title = "Overlay", icon = Icons.Filled.Layers) {
            ToggleRow(Icons.Filled.OpenWith, "Floating controls", floatingControls) { floatingControls = it }
            ToggleRow(Icons.Filled.RadioButtonUnchecked, "Circular lens", circularLens) { circularLens = it }
            ToggleRow(Icons.Filled.Save, "Save history", saveHistory) { saveHistory = it }
        }

        SettingsGroup(title = "Lens", icon = Icons.Filled.CenterFocusStrong) {
            ToggleRow(Icons.Filled.LightMode, "Edge glow", edgeGlow) { edgeGlow = it }
            PickerRow(
                icon = Icons.Filled.AutoAwesome,
                title = "Effect stack",
                value = activeEffectLabel(activeEffects),
                valueIcon = activeEffectIcon(activeEffects),
                onClick = {
                    val active = StackableLensEffects.filter { it in activeEffects }
                    val current = active.lastOrNull()
                    val nextIndex = if (current == null) {
                        0
                    } else {
                        (StackableLensEffects.indexOf(current) + 1) % StackableLensEffects.size
                    }
                    val next = StackableLensEffects[nextIndex]
                    onEffectSelected(next)
                }
            )
            PickerRow(
                icon = clarityMode.icon,
                title = "Clarity enhance",
                value = clarityMode.label,
                valueIcon = clarityMode.icon,
                onClick = { onClarityModeChange(clarityMode.next()) }
            )
            CompactSliderRow(
                icon = Icons.Filled.Search,
                title = "Zoom",
                valueLabel = String.format(Locale.US, "%.1fx", zoom),
                value = zoom,
                range = MIN_ZOOM..MAX_ZOOM,
                onValueChange = onZoomChange
            )
            CompactSliderRow(
                icon = Icons.Filled.Brightness5,
                title = "Intensity",
                valueLabel = "${(intensity * 100).toInt()}%",
                value = intensity,
                range = 0f..1f,
                onValueChange = onIntensityChange
            )
        }

        SettingsGroup(title = "Capture", icon = Icons.Filled.CameraAlt) {
            ToggleRow(Icons.Filled.PhotoCamera, "Show capture button", showCapture) { showCapture = it }
        }

        SettingsGroup(title = "Accessibility", icon = Icons.Filled.Accessibility) {
            ToggleRow(Icons.Filled.TextFields, "High contrast labels", highContrastLabels) { highContrastLabels = it }
        }
    }
}

@Composable
private fun SettingsGroup(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    GlassPanel(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RoundIconButton(
                    icon = icon,
                    contentDescription = null,
                    selected = false,
                    onClick = {},
                    size = 36.dp
                )
                Spacer(Modifier.width(9.dp))
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            content()
        }
    }
}

@Composable
private fun ColumnScope.ToggleRow(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    SettingRow(icon = icon, title = title) {
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFFE5EDF7),
                checkedTrackColor = ElectricBlue,
                uncheckedThumbColor = Color(0xFF7F8B98),
                uncheckedTrackColor = Color(0xFF101820)
            )
        )
    }
}

@Composable
private fun ColumnScope.PickerRow(
    icon: ImageVector,
    title: String,
    value: String,
    valueIcon: ImageVector,
    onClick: () -> Unit
) {
    SettingRow(icon = icon, title = title) {
        Surface(
            modifier = Modifier
                .widthIn(min = 128.dp)
                .height(38.dp)
                .clickable(role = Role.Button, onClick = onClick),
            shape = RoundedCornerShape(14.dp),
            color = Color(0xCC070B10),
            border = BorderStroke(1.dp, RimDim.copy(alpha = 0.9f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(valueIcon, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(16.dp))
                Text(
                    text = value,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun ColumnScope.CompactSliderRow(
    icon: ImageVector,
    title: String,
    valueLabel: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    SettingRow(icon = icon, title = title) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier.width(140.dp)
        ) {
            SmallRoundControl(
                icon = Icons.Filled.Remove,
                contentDescription = "Decrease $title",
                onClick = {
                    val step = if (range.endInclusive <= 1f) 0.05f else 0.1f
                    onValueChange((value - step).coerceAtLeast(range.start))
                },
                size = 32.dp
            )
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = range,
                colors = SliderDefaults.colors(
                    thumbColor = TextPrimary,
                    activeTrackColor = ElectricBlue,
                    inactiveTrackColor = Color(0xFF26313D)
                ),
                modifier = Modifier.weight(1f)
            )
            SmallRoundControl(
                icon = Icons.Filled.Add,
                contentDescription = "Increase $title",
                onClick = {
                    val step = if (range.endInclusive <= 1f) 0.05f else 0.1f
                    onValueChange((value + step).coerceAtMost(range.endInclusive))
                },
                size = 32.dp
            )
        }
        ValueBadge(valueLabel)
    }
}

@Composable
private fun ColumnScope.SettingRow(
    icon: ImageVector,
    title: String,
    trailing: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .border(0.5.dp, Color.White.copy(alpha = 0.07f), RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RoundIconButton(
            icon = icon,
            contentDescription = null,
            selected = false,
            onClick = {},
            size = 34.dp
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = title,
            color = TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        trailing()
    }
}

@Composable
private fun ValueBadge(text: String) {
    Surface(
        modifier = Modifier
            .width(56.dp)
            .height(34.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xCC070B10),
        border = BorderStroke(1.dp, RimDim.copy(alpha = 0.9f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                color = ElectricBlueSoft,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
private fun PageHeader(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            color = TextPrimary,
            fontSize = 34.sp,
            lineHeight = 38.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.sp
        )
        Text(
            text = subtitle,
            color = TextSecondary,
            fontSize = 14.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun BottomNavDock(
    selectedTab: AppTab,
    onSelected: (AppTab) -> Unit,
    modifier: Modifier = Modifier
) {
    GlassPanel(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppTab.entries.forEach { tab ->
                SquareActionButton(
                    label = tab.label,
                    icon = tab.icon,
                    selected = selectedTab == tab,
                    onClick = { onSelected(tab) },
                    modifier = Modifier.size(64.dp)
                )
            }
        }
    }
}

@Composable
private fun RoundIconButton(
    icon: ImageVector,
    contentDescription: String?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    enabled: Boolean = true,
    iconTint: Color = if (selected) ElectricBlueSoft else TextPrimary
) {
    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer { alpha = if (enabled) 1f else 0.5f }
            .shadow(if (selected) 10.dp else 6.dp, CircleShape)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    listOf(Color(0xFF1F2B36), Color(0xFF030609)),
                    radius = 120f
                )
            )
            .border(1.dp, if (selected) ElectricBlue else Rim.copy(alpha = 0.58f), CircleShape)
            .border(3.dp, if (selected) ElectricBlue.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.05f), CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconTint,
            modifier = Modifier.size(size * 0.48f)
        )
    }
}

@Composable
private fun GlassPanel(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape,
    selected: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .shadow(if (selected) 14.dp else 8.dp, shape)
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        if (selected) Color(0xFF122942) else AppPanelHigh,
                        AppPanel,
                        AppPanelLow
                    )
                )
            )
            .border(1.dp, if (selected) ElectricBlue else Rim.copy(alpha = 0.62f), shape)
            .border(2.dp, if (selected) ElectricBlue.copy(alpha = 0.24f) else Color.White.copy(alpha = 0.04f), shape)
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawRoundRect(
                color = Color.White.copy(alpha = if (selected) 0.2f else 0.1f),
                topLeft = Offset(size.width * 0.08f, 0f),
                size = Size(size.width * 0.84f, 1.3.dp.toPx()),
                cornerRadius = CornerRadius(8.dp.toPx())
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(ElectricBlue.copy(alpha = if (selected) 0.28f else 0.12f), Color.Transparent),
                    center = Offset(size.width * 0.5f, 0f),
                    radius = size.width * 0.44f
                ),
                radius = size.width * 0.44f,
                center = Offset(size.width * 0.5f, 0f)
            )
        }
        content()
    }
}

@Preview(
    name = "MagniZoom magnifier",
    showBackground = true,
    widthDp = 393,
    heightDp = 852
)
@Composable
private fun MagniZoomMagnifierPreview() {
    MagniZoomTheme(dynamicColor = false) {
        MagniZoomApp()
    }
}

@Preview(
    name = "Compact overlay",
    showBackground = true,
    widthDp = 206,
    heightDp = 216
)
@Composable
private fun MagniZoomOverlayPreview() {
    MagniZoomTheme(dynamicColor = false) {
        Box(Modifier.background(AppBlack)) {
            MagnifierOverlayContent()
        }
    }
}
