package com.example.engine.curl

import android.graphics.Bitmap
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.RectF
import android.graphics.Shader
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntSize
import com.example.ui.theme.FolioPalette
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

enum class CurlDirection { NONE, FORWARD, BACKWARD }
enum class CurlOriginMode { TOP_CORNER, MIDDLE_EDGE, BOTTOM_CORNER }

private enum class AnimPhase { IDLE, DRAGGING, COMPLETING, SPRINGING_BACK }

data class SelectionOverlayState(
    val rects: List<RectF>,
    val startHandle: PointF?,
    val endHandle: PointF?,
    val highlightColorArgb: Int
)

/**
 * Pure 2D Canvas Conical/Cylindrical Page-Curl Physics Engine (60fps, zero 3D libraries).
 * Renders both text-based books (EPUB/TXT) and PDF pages through the exact same bitmap pipeline.
 */
@Composable
fun PageCurlCanvas(
    currentBitmap: Bitmap?,
    prevBitmap: Bitmap?,
    nextBitmap: Bitmap?,
    currentPage: Int,
    totalPages: Int,
    palette: FolioPalette,
    pageStiffness: Float,
    selectionOverlay: SelectionOverlayState?,
    searchGlowRects: List<RectF>,
    searchGlowAlpha: Float,
    onSizeReady: (IntSize) -> Unit,
    onPageTurnCommit: (delta: Int) -> Unit,
    onPlayRustle: (velocityNorm: Float) -> Unit,
    onPageLandHaptic: () -> Unit,
    onCenterTap: () -> Unit,
    onLongPressAt: (Offset) -> Unit,
    onTapAtRestingPage: (Offset) -> Boolean, // returns true if consumed (e.g. tapped existing highlight or dismissed selection)
    onDragSelectionHandle: (isStartHandle: Boolean, position: Offset) -> Unit,
    oneHandedTapMode: Boolean = false,
    autoFlipTrigger: Int = 0,
    onLeftEdgeBrightnessDrag: (deltaFraction: Float) -> Unit = {},
    onLeftEdgeBrightnessEnd: () -> Unit = {},
    isPdf: Boolean = false,
    pdfZoomScale: Float = 1f,
    pdfPanOffset: Offset = Offset.Zero,
    onPdfPinchTransform: (scale: Float, pan: Offset) -> Unit = { _, _ -> },
    secretLineRects: List<RectF> = emptyList(),
    secretLineFrostAlpha: Float = 0f,
    onRubSecretLine: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current.density
    val touchSlop = LocalViewConfiguration.current.touchSlop
    val coroutineScope = rememberCoroutineScope()

    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var animPhase by remember { mutableStateOf(AnimPhase.IDLE) }
    var direction by remember { mutableStateOf(CurlDirection.NONE) }
    var originMode by remember { mutableStateOf(CurlOriginMode.BOTTOM_CORNER) }

    // Logical pointer coordinates controlling the fold crease
    var fingerX by remember { mutableFloatStateOf(0f) }
    var fingerY by remember { mutableFloatStateOf(0f) }

    // Animation interpolation state
    var animStartX by remember { mutableFloatStateOf(0f) }
    var animStartY by remember { mutableFloatStateOf(0f) }
    var animTargetX by remember { mutableFloatStateOf(0f) }
    var animTargetY by remember { mutableFloatStateOf(0f) }
    var animProgress by remember { mutableFloatStateOf(0f) }
    var animDurationMs by remember { mutableFloatStateOf(360f) }
    var isTapPeelArc by remember { mutableStateOf(false) }
    var frameTick by remember { mutableIntStateOf(0) }

    // Keep latest callbacks inside pointerInput without restarting gesture detector
    val latestCurrentPage by rememberUpdatedState(currentPage)
    val latestTotalPages by rememberUpdatedState(totalPages)
    val latestPrevBitmap by rememberUpdatedState(prevBitmap)
    val latestNextBitmap by rememberUpdatedState(nextBitmap)
    val latestStiffness by rememberUpdatedState(pageStiffness)
    val latestSelection by rememberUpdatedState(selectionOverlay)
    val latestOnCommit by rememberUpdatedState(onPageTurnCommit)
    val latestOnRustle by rememberUpdatedState(onPlayRustle)
    val latestOnHaptic by rememberUpdatedState(onPageLandHaptic)
    val latestOnCenterTap by rememberUpdatedState(onCenterTap)
    val latestOnLongPress by rememberUpdatedState(onLongPressAt)
    val latestOnRestingTap by rememberUpdatedState(onTapAtRestingPage)
    val latestOnDragHandle by rememberUpdatedState(onDragSelectionHandle)
    val latestOneHanded by rememberUpdatedState(oneHandedTapMode)
    val latestOnBrightnessDrag by rememberUpdatedState(onLeftEdgeBrightnessDrag)
    val latestOnBrightnessEnd by rememberUpdatedState(onLeftEdgeBrightnessEnd)
    val latestIsPdf by rememberUpdatedState(isPdf)
    val latestPdfZoom by rememberUpdatedState(pdfZoomScale)
    val latestPdfPan by rememberUpdatedState(pdfPanOffset)
    val latestOnPdfTransform by rememberUpdatedState(onPdfPinchTransform)
    val latestSecretRects by rememberUpdatedState(secretLineRects)
    val latestSecretFrost by rememberUpdatedState(secretLineFrostAlpha)
    val latestOnRubSecret by rememberUpdatedState(onRubSecretLine)

    // Reusable native Paint & Path objects (zero per-frame GC allocations)
    val drawResources = remember { CurlDrawResources() }

    // 60fps frame-synced animation driver for completing curls & tactile spring-back
    LaunchedEffect(animPhase) {
        if (animPhase != AnimPhase.COMPLETING && animPhase != AnimPhase.SPRINGING_BACK) return@LaunchedEffect
        val w = canvasSize.width.toFloat().coerceAtLeast(1f)
        val h = canvasSize.height.toFloat().coerceAtLeast(1f)
        var lastNanos = 0L
        animProgress = 0f

        while (isActive && (animPhase == AnimPhase.COMPLETING || animPhase == AnimPhase.SPRINGING_BACK)) {
            withFrameNanos { frameNanos ->
                if (lastNanos == 0L) {
                    lastNanos = frameNanos
                    return@withFrameNanos
                }
                val dtMs = ((frameNanos - lastNanos) / 1_000_000f).coerceIn(1f, 34f)
                lastNanos = frameNanos
                animProgress = (animProgress + dtMs / animDurationMs).coerceIn(0f, 1f)

                val t = animProgress
                val eased = if (animPhase == AnimPhase.SPRINGING_BACK) {
                    // Tactile damped spring-back curve scaled by stiffness
                    val damping = 6.2f + latestStiffness * 3.0f
                    val freq = 9.0f + latestStiffness * 4.0f
                    val spring = 1f - kotlin.math.exp((-damping * t).toDouble()).toFloat() *
                        kotlin.math.cos((freq * t).toDouble()).toFloat()
                    spring.coerceIn(0f, 1.03f)
                } else {
                    // Smooth cubic ease-out for natural page turn inertia
                    1f - (1f - t).pow(3)
                }

                fingerX = animStartX + (animTargetX - animStartX) * eased
                val baseY = animStartY + (animTargetY - animStartY) * eased
                // Add graceful curved corner-peel lift arc during tap-to-flip
                val arcLift = if (isTapPeelArc) {
                    val liftAmp = h * (0.14f - latestStiffness * 0.04f)
                    -sin(t * PI).toFloat() * liftAmp
                } else {
                    0f
                }
                fingerY = (baseY + arcLift).coerceIn(4f, h - 4f)
                frameTick++

                if (animProgress >= 1f) {
                    if (animPhase == AnimPhase.COMPLETING) {
                        val delta = if (direction == CurlDirection.FORWARD) 1 else -1
                        latestOnCommit(delta)
                        latestOnHaptic()
                    }
                    animPhase = AnimPhase.IDLE
                    direction = CurlDirection.NONE
                    isTapPeelArc = false
                }
            }
        }
    }

    fun startTapFlip(forward: Boolean) {
        val w = canvasSize.width.toFloat()
        val h = canvasSize.height.toFloat()
        if (w <= 1f || h <= 1f) return
        if (forward && latestCurrentPage >= latestTotalPages - 1) return
        if (!forward && latestCurrentPage <= 0) return

        direction = if (forward) CurlDirection.FORWARD else CurlDirection.BACKWARD
        originMode = CurlOriginMode.BOTTOM_CORNER
        isTapPeelArc = true
        animDurationMs = (420f - latestStiffness * 90f).coerceIn(290f, 440f)

        if (forward) {
            animStartX = w * 0.96f
            animStartY = h * 0.88f
            animTargetX = -w * 0.04f
            animTargetY = h * 0.80f
        } else {
            animStartX = -w * 0.02f
            animStartY = h * 0.82f
            animTargetX = w * 1.01f
            animTargetY = h * 0.88f
        }
        fingerX = animStartX
        fingerY = animStartY
        latestOnRustle(0.82f)
        animPhase = AnimPhase.COMPLETING
    }

    // Auto-Flip Timer trigger (smoothly plays curved corner-peel turn)
    LaunchedEffect(autoFlipTrigger) {
        if (autoFlipTrigger > 0 && animPhase == AnimPhase.IDLE) {
            startTapFlip(forward = true)
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .testTag("reader_page_curl_canvas")
            .onSizeChanged { sz ->
                canvasSize = sz
                if (sz.width > 0 && sz.height > 0) {
                    onSizeReady(sz)
                }
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val w = size.width.toFloat().coerceAtLeast(1f)
                    val h = size.height.toFloat().coerceAtLeast(1f)
                    val startPos = down.position

                    // Ignore new drags while an animation is mid-flight
                    if (animPhase == AnimPhase.COMPLETING || animPhase == AnimPhase.SPRINGING_BACK) {
                        return@awaitEachGesture
                    }

                    // 1. Check if grabbing a draggable text selection handle on a resting page
                    val activeSel = latestSelection
                    val handleHitRadius = 32f * density
                    val grabbingStartHandle = activeSel?.startHandle?.let {
                        hypot((startPos.x - it.x).toDouble(), (startPos.y - it.y).toDouble()) <= handleHitRadius
                    } == true
                    val grabbingEndHandle = !grabbingStartHandle && activeSel?.endHandle?.let {
                        hypot((startPos.x - it.x).toDouble(), (startPos.y - it.y).toDouble()) <= handleHitRadius
                    } == true

                    if (grabbingStartHandle || grabbingEndHandle) {
                        down.consume()
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Main)
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) {
                                change.consume()
                                break
                            }
                            latestOnDragHandle(grabbingStartHandle, change.position)
                            change.consume()
                        }
                        return@awaitEachGesture
                    }

                    // 2. Normal reading gesture: distinguish long-press, left-edge brightness, PDF pinch/pan, secret frost rub, tap, or page-curl drag
                    var isDraggingCurl = false
                    var isDraggingBrightness = false
                    var isPinchingPdf = false
                    var isPanningPdf = false
                    var isRubbingSecret = false
                    var longPressTriggered = false
                    val velocityTracker = VelocityTracker()
                    velocityTracker.addPosition(down.uptimeMillis, startPos)

                    val longPressJob = coroutineScope.launch {
                        delay(390L)
                        if (!isDraggingCurl && !isDraggingBrightness && !isPinchingPdf && !isPanningPdf && !isRubbingSecret) {
                            longPressTriggered = true
                            latestOnLongPress(startPos)
                        }
                    }

                    var lastPos = startPos
                    var prevPinchSpan = 0f
                    var prevPinchCentroid = Offset.Zero
                    var rustlePlayed = false

                    try {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Main)
                            val activePointers = event.changes.filter { it.pressed }

                            // Two-finger PDF Pinch-to-Zoom
                            if (latestIsPdf && activePointers.size >= 2 && !isDraggingCurl) {
                                longPressJob.cancel()
                                val p1 = activePointers[0].position
                                val p2 = activePointers[1].position
                                val span = hypot((p2.x - p1.x).toDouble(), (p2.y - p1.y).toDouble()).toFloat().coerceAtLeast(1f)
                                val centroid = Offset((p1.x + p2.x) * 0.5f, (p1.y + p2.y) * 0.5f)
                                if (!isPinchingPdf) {
                                    isPinchingPdf = true
                                    prevPinchSpan = span
                                    prevPinchCentroid = centroid
                                } else {
                                    val zoomDelta = (span / prevPinchSpan).coerceIn(0.8f, 1.25f)
                                    val newScale = (latestPdfZoom * zoomDelta).coerceIn(1.0f, 2.8f)
                                    val panDelta = centroid - prevPinchCentroid
                                    val maxPanX = w * (newScale - 1f) * 0.5f
                                    val maxPanY = h * (newScale - 1f) * 0.5f
                                    val newPan = if (newScale <= 1.02f) Offset.Zero else Offset(
                                        (latestPdfPan.x + panDelta.x).coerceIn(-maxPanX, maxPanX),
                                        (latestPdfPan.y + panDelta.y).coerceIn(-maxPanY, maxPanY)
                                    )
                                    latestOnPdfTransform(newScale, newPan)
                                    prevPinchSpan = span
                                    prevPinchCentroid = centroid
                                }
                                activePointers.forEach { it.consume() }
                                continue
                            }

                            val change = event.changes.firstOrNull { it.id == down.id } ?: break

                            if (!change.pressed) {
                                longPressJob.cancel()
                                if (isDraggingBrightness) {
                                    latestOnBrightnessEnd()
                                } else if (isPinchingPdf || isPanningPdf || isRubbingSecret) {
                                    // Finished pinch/pan/frost rub without page flip
                                } else if (isDraggingCurl) {
                                    val velX = velocityTracker.calculateVelocity().x
                                    val dragDistFraction = when (direction) {
                                        CurlDirection.FORWARD -> ((w - fingerX) / w).coerceIn(0f, 1f)
                                        CurlDirection.BACKWARD -> (fingerX / w).coerceIn(0f, 1f)
                                        CurlDirection.NONE -> 0f
                                    }

                                    // Commit if dragged >= 35% OR fast velocity flick
                                    val shouldComplete = when (direction) {
                                        CurlDirection.FORWARD -> dragDistFraction >= 0.35f || velX < -620f
                                        CurlDirection.BACKWARD -> dragDistFraction >= 0.35f || velX > 620f
                                        CurlDirection.NONE -> false
                                    }

                                    animStartX = fingerX
                                    animStartY = fingerY
                                    isTapPeelArc = false

                                    if (shouldComplete) {
                                        val speedNorm = (abs(velX) / 1600f).coerceIn(0.45f, 1.4f)
                                        if (!rustlePlayed) {
                                            latestOnRustle(speedNorm)
                                        }
                                        animDurationMs = (310f - speedNorm * 85f).coerceIn(170f, 340f)
                                        if (direction == CurlDirection.FORWARD) {
                                            animTargetX = -w * 0.04f
                                            animTargetY = when (originMode) {
                                                CurlOriginMode.TOP_CORNER -> h * 0.14f
                                                CurlOriginMode.BOTTOM_CORNER -> h * 0.86f
                                                CurlOriginMode.MIDDLE_EDGE -> h * 0.5f
                                            }
                                        } else {
                                            animTargetX = w * 1.02f
                                            animTargetY = when (originMode) {
                                                CurlOriginMode.TOP_CORNER -> h * 0.08f
                                                CurlOriginMode.BOTTOM_CORNER -> h * 0.92f
                                                CurlOriginMode.MIDDLE_EDGE -> h * 0.5f
                                            }
                                        }
                                        animPhase = AnimPhase.COMPLETING
                                    } else {
                                        // Smooth tactile spring-back when released < 35%
                                        animDurationMs = (320f - latestStiffness * 70f).coerceIn(210f, 350f)
                                        if (direction == CurlDirection.FORWARD) {
                                            animTargetX = w * 1.01f
                                            animTargetY = when (originMode) {
                                                CurlOriginMode.TOP_CORNER -> 0f
                                                CurlOriginMode.BOTTOM_CORNER -> h
                                                CurlOriginMode.MIDDLE_EDGE -> h * 0.5f
                                            }
                                        } else {
                                            animTargetX = -w * 0.03f
                                            animTargetY = when (originMode) {
                                                CurlOriginMode.TOP_CORNER -> h * 0.12f
                                                CurlOriginMode.BOTTOM_CORNER -> h * 0.88f
                                                CurlOriginMode.MIDDLE_EDGE -> h * 0.5f
                                            }
                                        }
                                        animPhase = AnimPhase.SPRINGING_BACK
                                    }
                                } else if (!longPressTriggered) {
                                    // Tap handling on resting page
                                    val tappedFrost = latestSecretFrost > 0.05f &&
                                        latestSecretRects.any { r ->
                                            startPos.x >= r.left - 14f && startPos.x <= r.right + 14f &&
                                                startPos.y >= r.top - 14f && startPos.y <= r.bottom + 14f
                                        }
                                    if (tappedFrost) {
                                        latestOnRubSecret()
                                    } else {
                                        val consumedBySelectionOrHighlight = latestOnRestingTap(startPos)
                                        if (!consumedBySelectionOrHighlight) {
                                            when {
                                                startPos.x >= w * 0.80f -> startTapFlip(forward = true)
                                                startPos.x <= w * 0.20f -> startTapFlip(forward = latestOneHanded)
                                                else -> latestOnCenterTap()
                                            }
                                        }
                                    }
                                }
                                change.consume()
                                break
                            }

                            val prevPos = lastPos
                            lastPos = change.position
                            velocityTracker.addPosition(change.uptimeMillis, lastPos)
                            val dx = lastPos.x - startPos.x
                            val dy = lastPos.y - startPos.y

                            if (!isDraggingCurl && !isDraggingBrightness && !isPinchingPdf && !isPanningPdf && !isRubbingSecret &&
                                !longPressTriggered && hypot(dx.toDouble(), dy.toDouble()) > touchSlop
                            ) {
                                longPressJob.cancel()

                                // If text selection is open, ignore horizontal curls until dismissed
                                if (latestSelection != null) {
                                    continue
                                }

                                // Check gentle rub across frosted-glass secret line (Featured Story)
                                val nearSecret = latestSecretFrost > 0.05f &&
                                    latestSecretRects.any { r ->
                                        startPos.x >= r.left - 24f && startPos.x <= r.right + 24f &&
                                            startPos.y >= r.top - 24f && startPos.y <= r.bottom + 24f
                                    }
                                if (nearSecret) {
                                    isRubbingSecret = true
                                    latestOnRubSecret()
                                    change.consume()
                                    continue
                                }

                                // Far-left 12% vertical slide -> in-app brightness/dimmer gesture
                                // (horizontal drag still falls through to page-curl!)
                                if (startPos.x <= w * 0.12f && abs(dy) > abs(dx) * 1.25f) {
                                    isDraggingBrightness = true
                                } else if (latestIsPdf && latestPdfZoom > 1.06f && startPos.x in (w * 0.14f)..(w * 0.86f)) {
                                    // Pan zoomed PDF when dragging inside central area
                                    isPanningPdf = true
                                } else {
                                    // Determine forward vs backward curl from drag direction & edge proximity
                                    val wantForward = dx < -4f && latestCurrentPage < latestTotalPages - 1 && latestNextBitmap != null
                                    val wantBackward = dx > 4f && latestCurrentPage > 0 && latestPrevBitmap != null

                                    if (wantForward || wantBackward) {
                                        isDraggingCurl = true
                                        direction = if (wantForward) CurlDirection.FORWARD else CurlDirection.BACKWARD
                                        originMode = when {
                                            startPos.y < h * 0.30f -> CurlOriginMode.TOP_CORNER
                                            startPos.y > h * 0.70f -> CurlOriginMode.BOTTOM_CORNER
                                            else -> CurlOriginMode.MIDDLE_EDGE
                                        }
                                        animPhase = AnimPhase.DRAGGING
                                    }
                                }
                            }

                            if (isDraggingBrightness) {
                                val stepDy = prevPos.y - lastPos.y // swipe up = brighter
                                latestOnBrightnessDrag(stepDy / (h * 0.65f))
                                change.consume()
                            } else if (isPanningPdf) {
                                val step = lastPos - prevPos
                                val maxPanX = w * (latestPdfZoom - 1f) * 0.5f
                                val maxPanY = h * (latestPdfZoom - 1f) * 0.5f
                                val nextPan = Offset(
                                    (latestPdfPan.x + step.x).coerceIn(-maxPanX, maxPanX),
                                    (latestPdfPan.y + step.y).coerceIn(-maxPanY, maxPanY)
                                )
                                latestOnPdfTransform(latestPdfZoom, nextPan)
                                change.consume()
                            } else if (isRubbingSecret) {
                                latestOnRubSecret()
                                change.consume()
                            } else if (isDraggingCurl) {
                                // Apply subtle paper stiffness damping on Y deflection for tactile feel
                                val stiffnessBlend = 1f - (latestStiffness - 0.2f) * 0.28f
                                fingerX = lastPos.x.coerceIn(-w * 0.05f, w * 1.02f)
                                fingerY = when (originMode) {
                                    CurlOriginMode.MIDDLE_EDGE -> h * 0.5f
                                    CurlOriginMode.TOP_CORNER -> {
                                        val rawY = lastPos.y.coerceIn(4f, h * 0.72f)
                                        rawY * stiffnessBlend
                                    }
                                    CurlOriginMode.BOTTOM_CORNER -> {
                                        val rawY = lastPos.y.coerceIn(h * 0.28f, h - 4f)
                                        h - (h - rawY) * stiffnessBlend
                                    }
                                }

                                // Play gentle rustle once dragged past 22% with motion
                                val progress = if (direction == CurlDirection.FORWARD) {
                                    (w - fingerX) / w
                                } else {
                                    fingerX / w
                                }
                                if (!rustlePlayed && progress > 0.22f) {
                                    rustlePlayed = true
                                    val speedNorm = (abs(velocityTracker.calculateVelocity().x) / 1400f).coerceIn(0.45f, 1.3f)
                                    latestOnRustle(speedNorm)
                                }

                                frameTick++
                                change.consume()
                            }
                        }
                    } finally {
                        longPressJob.cancel()
                    }
                }
            }
    ) {
        // Touch frameTick so Compose invalidates draw on every physics frame
        val unusedTick = frameTick
        val w = size.width
        val h = size.height
        if (w <= 1f || h <= 1f) return@Canvas

        drawIntoCanvas { composeCanvas ->
            val canvas = composeCanvas.nativeCanvas
            val progressFrac = if (totalPages <= 1) 0f else (currentPage.toFloat() / (totalPages - 1)).coerceIn(0f, 1f)

            val isCurling = animPhase != AnimPhase.IDLE && direction != CurlDirection.NONE
            val frontBmp: Bitmap?
            val underBmp: Bitmap?
            if (isCurling && direction == CurlDirection.BACKWARD) {
                frontBmp = prevBitmap
                underBmp = currentBitmap
            } else {
                frontBmp = currentBitmap
                underBmp = nextBitmap
            }

            if (frontBmp == null) {
                canvas.drawColor(palette.background.toArgb())
                return@drawIntoCanvas
            }

            if (!isCurling || underBmp == null) {
                // Resting flat page (with optional locked/live PDF pinch-zoom transform)
                if (isPdf && pdfZoomScale > 1.01f) {
                    canvas.save()
                    canvas.translate(w * 0.5f + pdfPanOffset.x, h * 0.5f + pdfPanOffset.y)
                    canvas.scale(pdfZoomScale, pdfZoomScale)
                    canvas.translate(-w * 0.5f, -h * 0.5f)
                    canvas.drawBitmap(frontBmp, 0f, 0f, drawResources.bitmapPaint)
                    canvas.restore()
                } else {
                    canvas.drawBitmap(frontBmp, 0f, 0f, null)
                }

                // Resting overlays: search-match glow pulse, active text selection, & featured frosted secret line
                drawRestingOverlays(
                    canvas = canvas,
                    selectionOverlay = selectionOverlay,
                    searchGlowRects = searchGlowRects,
                    searchGlowAlpha = searchGlowAlpha,
                    secretLineRects = secretLineRects,
                    secretLineFrostAlpha = secretLineFrostAlpha,
                    palette = palette,
                    density = density,
                    res = drawResources
                )

                // Inner spine/gutter shadow & dynamic left/right page stacks
                drawSpineShadowAndPageStack(
                    canvas = canvas,
                    w = w,
                    h = h,
                    progressFraction = progressFrac,
                    palette = palette,
                    density = density,
                    res = drawResources
                )
            } else {
                // Active 60fps 2D Conical/Cylindrical Page Curl!
                drawConicalPageCurl(
                    canvas = canvas,
                    w = w,
                    h = h,
                    frontBmp = frontBmp,
                    underBmp = underBmp,
                    fingerX = fingerX,
                    fingerY = fingerY,
                    originMode = originMode,
                    stiffness = pageStiffness,
                    palette = palette,
                    density = density,
                    progressFraction = progressFrac,
                    res = drawResources
                )
            }
        }
    }
}

private class CurlDrawResources {
    val peeledPath = Path()
    val backPath = Path()
    val frontClipPath = Path()
    val reflectMatrix = Matrix()
    val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    val ghostPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
        alpha = 31 // ~12% opacity mirrored front-page ghosting on backside
    }
    val backPaperPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    val shadePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    val stackLinePaint = Paint().apply { strokeWidth = 1f }
    val overlayPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    val handlePaint = Paint(Paint.ANTI_ALIAS_FLAG)
}

private fun drawConicalPageCurl(
    canvas: android.graphics.Canvas,
    w: Float,
    h: Float,
    frontBmp: Bitmap,
    underBmp: Bitmap,
    fingerX: Float,
    fingerY: Float,
    originMode: CurlOriginMode,
    stiffness: Float,
    palette: FolioPalette,
    density: Float,
    progressFraction: Float,
    res: CurlDrawResources
) {
    // 1. Anchor corner C on right edge
    val anchorY = when (originMode) {
        CurlOriginMode.TOP_CORNER -> 0f
        CurlOriginMode.BOTTOM_CORNER -> h
        CurlOriginMode.MIDDLE_EDGE -> h * 0.5f
    }
    val cx = w
    val cy = anchorY

    // 2. Map finger position to non-stretching folded corner tip T
    // M_x = (W + tx)/2 = fingerX => tx = 2*fingerX - W
    var tx = (2f * fingerX - w).coerceIn(-w * 0.99f, w - 2.5f)
    var ty = when (originMode) {
        CurlOriginMode.MIDDLE_EDGE -> h * 0.5f
        CurlOriginMode.TOP_CORNER -> {
            // Pull corner gently downward & inward as user curls from top-right
            val dy = (fingerY - 0f).coerceAtLeast(1.5f)
            (dy * 1.45f).coerceIn(2f, h * 0.85f)
        }
        CurlOriginMode.BOTTOM_CORNER -> {
            val dy = (h - fingerY).coerceAtLeast(1.5f)
            (h - dy * 1.45f).coerceIn(h * 0.15f, h - 2f)
        }
    }

    // 3. Physical spine constraints (paper cannot stretch beyond left spine x = 0)
    if (originMode != CurlOriginMode.MIDDLE_EDGE) {
        // Constraint A: Distance from same-side spine pivot (0, cy) <= W
        val dx1 = tx - 0f
        val dy1 = ty - cy
        val dist1 = sqrt(dx1 * dx1 + dy1 * dy1)
        val maxR1 = w - 1f
        if (dist1 > maxR1 && dist1 > 0.001f) {
            val scale = maxR1 / dist1
            tx = dx1 * scale
            ty = cy + dy1 * scale
        }

        // Constraint B: Distance from opposite spine corner (0, H - cy) <= diagonal
        val oppY = h - cy
        val dx2 = tx - 0f
        val dy2 = ty - oppY
        val dist2 = sqrt(dx2 * dx2 + dy2 * dy2)
        val maxR2 = sqrt(w * w + h * h) - 1f
        if (dist2 > maxR2 && dist2 > 0.001f) {
            val scale = maxR2 / dist2
            tx = dx2 * scale
            ty = oppY + dy2 * scale
        }
    }

    // 4. Fold crease line L: perpendicular bisector of C(cx, cy) and T(tx, ty)
    val vcx = cx - tx
    val vcy = cy - ty
    val ctDist = sqrt(vcx * vcx + vcy * vcy)
    if (ctDist < 3f) {
        canvas.drawBitmap(frontBmp, 0f, 0f, null)
        drawSpineShadowAndPageStack(canvas, w, h, progressFraction, palette, density, res)
        return
    }

    // Unit normal n pointing from T toward C (into revealed under-page)
    val nx = vcx / ctDist
    val ny = vcy / ctDist
    val mx = (cx + tx) * 0.5f
    val my = (cy + ty) * 0.5f

    // Signed distance helper: s(P) = (P - M) . n
    val mDotN = mx * nx + my * ny

    // Page corners clockwise
    val rectVerts = listOf(
        PointF(0f, 0f),
        PointF(w, 0f),
        PointF(w, h),
        PointF(0f, h)
    )

    // Clip page rect to C-side (s >= 0) -> revealed under-page polygon
    val peeledPoly = clipPolygonHalfPlane(rectVerts, nx, ny, mDotN, keepPositive = true)
    // Clip page rect to spine-side (s <= 0) -> remaining flat front-page polygon
    val flatFrontPoly = clipPolygonHalfPlane(rectVerts, nx, ny, mDotN, keepPositive = false)

    if (peeledPoly.size < 3) {
        canvas.drawBitmap(frontBmp, 0f, 0f, null)
        drawSpineShadowAndPageStack(canvas, w, h, progressFraction, palette, density, res)
        return
    }

    // Reflect peeledPoly vertices across fold line L -> curled backside flap!
    val backPoly = peeledPoly.map { p ->
        val s = p.x * nx + p.y * ny - mDotN
        PointF(p.x - 2f * s * nx, p.y - 2f * s * ny)
    }

    // Compute maximum fold extent (for dynamic elevation lighting & cylindrical shading)
    var maxExtent = 1f
    for (p in peeledPoly) {
        val s = p.x * nx + p.y * ny - mDotN
        if (s > maxExtent) maxExtent = s
    }
    // Elevation peaks mid-curl and softly flattens to 0 at start and end of turn
    val turnRatio = ((w - fingerX) / w).coerceIn(0f, 1f)
    val elevation = sin(turnRatio * PI).toFloat().coerceIn(0.06f, 1f)
    val isDark = palette.isDark

    // Build native Paths
    buildPathFromPoly(res.peeledPath, peeledPoly)
    buildPathFromPoly(res.backPath, backPoly)
    buildPathFromPoly(res.frontClipPath, flatFrontPoly)

    // --- LAYER 1: Resting left portion of Front Page ---
    canvas.save()
    if (flatFrontPoly.size >= 3) {
        canvas.clipPath(res.frontClipPath)
    }
    canvas.drawBitmap(frontBmp, 0f, 0f, res.bitmapPaint)

    // Soft cast shadow from the curled flap onto the flat front page beneath it
    val flapShadowReach = (maxExtent * 1.12f + 22f * density * elevation).coerceAtLeast(18f * density)
    val frontShadowAlpha = if (isDark) (110 * elevation).toInt() else (68 * elevation).toInt()
    res.shadowPaint.shader = LinearGradient(
        mx, my,
        mx - nx * flapShadowReach, my - ny * flapShadowReach,
        intArrayOf(
            android.graphics.Color.argb(frontShadowAlpha, 0, 0, 0),
            android.graphics.Color.argb((frontShadowAlpha * 0.45f).toInt(), 0, 0, 0),
            android.graphics.Color.TRANSPARENT
        ),
        floatArrayOf(0f, 0.55f, 1f),
        Shader.TileMode.CLAMP
    )
    canvas.drawRect(0f, 0f, w, h, res.shadowPaint)
    canvas.restore()

    // --- LAYER 2: Revealed Under-Page (Next Page) + Elevation-Aware Drop Shadow ---
    canvas.save()
    canvas.clipPath(res.peeledPath)
    canvas.drawBitmap(underBmp, 0f, 0f, res.bitmapPaint)

    val dropShadowWidth = ((22f + 68f * elevation) * density * (1.15f - stiffness * 0.25f))
        .coerceIn(14f * density, w * 0.42f)
    val dropAlphaStrong = if (isDark) (165 * elevation).toInt() else (115 * elevation).toInt()
    val dropAlphaMid = (dropAlphaStrong * 0.42f).toInt()
    res.shadowPaint.shader = LinearGradient(
        mx, my,
        mx + nx * dropShadowWidth, my + ny * dropShadowWidth,
        intArrayOf(
            android.graphics.Color.argb(dropAlphaStrong, 0, 0, 0),
            android.graphics.Color.argb(dropAlphaMid, 0, 0, 0),
            android.graphics.Color.TRANSPARENT
        ),
        floatArrayOf(0f, 0.34f, 1f),
        Shader.TileMode.CLAMP
    )
    canvas.drawPath(res.peeledPath, res.shadowPaint)
    canvas.restore()

    // --- LAYER 3: Curled Backside of Turning Page (Mirrored 12% Ghosting + Specular Fold Ridge) ---
    canvas.save()
    canvas.clipPath(res.backPath)

    // 3a. Slightly shaded backside paper base
    res.backPaperPaint.color = palette.pageBackside.toArgb()
    canvas.drawPath(res.backPath, res.backPaperPaint)

    // 3b. Back-of-Page Ghosting: exact 2D reflection of frontBmp across fold line L at ~12% opacity
    val m00 = 1f - 2f * nx * nx
    val m01 = -2f * nx * ny
    val m02 = 2f * mDotN * nx
    val m10 = -2f * nx * ny
    val m11 = 1f - 2f * ny * ny
    val m12 = 2f * mDotN * ny
    res.reflectMatrix.setValues(
        floatArrayOf(
            m00, m01, m02,
            m10, m11, m12,
            0f, 0f, 1f
        )
    )
    canvas.save()
    canvas.concat(res.reflectMatrix)
    res.ghostPaint.alpha = if (isDark) 34 else 31 // ~12% opacity ink bleed-through
    canvas.drawBitmap(frontBmp, 0f, 0f, res.ghostPaint)
    canvas.restore()

    // 3c. Cylindrical Shading + Soft Specular Highlight along the curved fold ridge
    // Cylinder width scales with page stiffness (stiffer paper = broader smooth curve)
    val cylSpan = (maxExtent * (0.85f + stiffness * 0.25f)).coerceAtLeast(24f * density)
    val highlightColor = if (isDark) {
        android.graphics.Color.argb((34 * elevation).toInt().coerceIn(8, 38), 235, 232, 224)
    } else {
        android.graphics.Color.argb((120 * elevation).toInt().coerceIn(30, 128), 255, 253, 248)
    }
    val troughShadow = android.graphics.Color.argb(
        if (isDark) (95 * elevation).toInt() else (62 * elevation).toInt(),
        0, 0, 0
    )
    val bodyShade = android.graphics.Color.argb(
        if (isDark) 34 else 26,
        0, 0, 0
    )
    val edgeShade = android.graphics.Color.argb(
        if (isDark) 52 else 42,
        0, 0, 0
    )

    res.shadePaint.shader = LinearGradient(
        mx, my,
        mx - nx * cylSpan, my - ny * cylSpan,
        intArrayOf(troughShadow, highlightColor, android.graphics.Color.TRANSPARENT, bodyShade, edgeShade),
        floatArrayOf(0f, 0.14f, 0.36f, 0.72f, 1f),
        Shader.TileMode.CLAMP
    )
    canvas.drawPath(res.backPath, res.shadePaint)

    // Subtle crisp paper-edge rim line on the curled flap
    res.overlayPaint.style = Paint.Style.STROKE
    res.overlayPaint.strokeWidth = 0.8f * density
    res.overlayPaint.color = if (isDark) {
        android.graphics.Color.argb(38, 212, 210, 204)
    } else {
        android.graphics.Color.argb(42, 43, 33, 24)
    }
    canvas.drawPath(res.backPath, res.overlayPaint)
    res.overlayPaint.style = Paint.Style.FILL
    canvas.restore()

    // --- LAYER 4: Subtle Inner Gutter/Spine Shadow & Dynamic Page Stack ---
    drawSpineShadowAndPageStack(
        canvas = canvas,
        w = w,
        h = h,
        progressFraction = progressFraction,
        palette = palette,
        density = density,
        res = res
    )
}

private fun drawSpineShadowAndPageStack(
    canvas: android.graphics.Canvas,
    w: Float,
    h: Float,
    progressFraction: Float,
    palette: FolioPalette,
    density: Float,
    res: CurlDrawResources
) {
    val isDark = palette.isDark

    // 1. Subtle inner gutter / spine shadow along the left binding edge
    val gutterWidth = 18f * density
    val gutterAlpha = if (isDark) 68 else 32
    res.shadowPaint.shader = LinearGradient(
        0f, 0f,
        gutterWidth, 0f,
        intArrayOf(
            android.graphics.Color.argb(gutterAlpha, 0, 0, 0),
            android.graphics.Color.argb((gutterAlpha * 0.35f).toInt(), 0, 0, 0),
            android.graphics.Color.TRANSPARENT
        ),
        floatArrayOf(0f, 0.42f, 1f),
        Shader.TileMode.CLAMP
    )
    canvas.drawRect(0f, 0f, gutterWidth, h, res.shadowPaint)

    // 2. Dynamic Page Stack: ultra-thin stacked leaf edges that thicken on left & thin on right
    val maxStackLines = 7
    val leftLines = (progressFraction * maxStackLines).toInt().coerceIn(0, maxStackLines)
    val rightLines = ((1f - progressFraction) * maxStackLines).toInt().coerceIn(0, maxStackLines)
    val stepPx = (1.15f * density).coerceAtLeast(1.5f)

    val lineShadowColor = if (isDark) {
        android.graphics.Color.argb(42, 212, 210, 204)
    } else {
        android.graphics.Color.argb(38, 43, 33, 24)
    }
    val linePaperColor = palette.pageBackside.toArgb()

    // Left stack (read pages accumulating)
    if (leftLines > 0) {
        val stackW = leftLines * stepPx
        res.backPaperPaint.color = linePaperColor
        canvas.drawRect(0f, 0f, stackW, h, res.backPaperPaint)
        res.stackLinePaint.color = lineShadowColor
        res.stackLinePaint.strokeWidth = (0.7f * density).coerceAtLeast(1f)
        for (i in 1..leftLines) {
            val x = i * stepPx
            canvas.drawLine(x, 0f, x, h, res.stackLinePaint)
        }
    }

    // Right stack (unread pages thinning)
    if (rightLines > 0) {
        val stackW = rightLines * stepPx
        res.backPaperPaint.color = linePaperColor
        canvas.drawRect(w - stackW, 0f, w, h, res.backPaperPaint)
        res.stackLinePaint.color = lineShadowColor
        res.stackLinePaint.strokeWidth = (0.7f * density).coerceAtLeast(1f)
        for (i in 1..rightLines) {
            val x = w - i * stepPx
            canvas.drawLine(x, 0f, x, h, res.stackLinePaint)
        }
    }
}

private fun drawRestingOverlays(
    canvas: android.graphics.Canvas,
    selectionOverlay: SelectionOverlayState?,
    searchGlowRects: List<RectF>,
    searchGlowAlpha: Float,
    secretLineRects: List<RectF>,
    secretLineFrostAlpha: Float,
    palette: FolioPalette,
    density: Float,
    res: CurlDrawResources
) {
    // Featured Story Exclusive: Frosted-glass fogged veil over secret sentence (tap or rub to clear)
    if (secretLineFrostAlpha > 0.01f && secretLineRects.isNotEmpty()) {
        val radius = 6f * density
        val frostBase = palette.pageBackside.toArgb()
        val frostSheen = if (palette.isDark) {
            android.graphics.Color.argb((secretLineFrostAlpha * 238f).toInt().coerceIn(0, 245), 34, 34, 40)
        } else {
            android.graphics.Color.argb((secretLineFrostAlpha * 238f).toInt().coerceIn(0, 245), 243, 240, 234)
        }
        res.overlayPaint.style = Paint.Style.FILL
        secretLineRects.forEach { rect ->
            val padded = RectF(
                rect.left - 4f * density,
                rect.top - 2f * density,
                rect.right + 4f * density,
                rect.bottom + 2f * density
            )
            // Opaque fog base hiding the underlying text like condensed glass
            res.overlayPaint.color = frostBase
            res.overlayPaint.alpha = (secretLineFrostAlpha * 242f).toInt().coerceIn(0, 245)
            canvas.drawRoundRect(padded, radius, radius, res.overlayPaint)

            // Frosted-glass cool sheen layer
            res.overlayPaint.color = frostSheen
            canvas.drawRoundRect(padded, radius, radius, res.overlayPaint)

            // Delicate warm-gold frosted border cue
            res.overlayPaint.style = Paint.Style.STROKE
            res.overlayPaint.strokeWidth = 0.9f * density
            res.overlayPaint.color = 0xFFD2A86A.toInt()
            res.overlayPaint.alpha = (secretLineFrostAlpha * 135f).toInt().coerceIn(0, 150)
            canvas.drawRoundRect(padded, radius, radius, res.overlayPaint)
            res.overlayPaint.style = Paint.Style.FILL
        }
    }

    // Search-match warm pulsing glow
    if (searchGlowAlpha > 0.01f && searchGlowRects.isNotEmpty()) {
        val glowColor = palette.accent.toArgb()
        res.overlayPaint.style = Paint.Style.FILL
        res.overlayPaint.color = glowColor
        res.overlayPaint.alpha = (searchGlowAlpha * 115f).toInt().coerceIn(0, 140)
        val r = 5f * density
        searchGlowRects.forEach { rect ->
            val padded = RectF(rect.left - 3f * density, rect.top - 2f * density, rect.right + 3f * density, rect.bottom + 2f * density)
            canvas.drawRoundRect(padded, r, r, res.overlayPaint)
        }
    }

    // Active text selection + draggable handles
    if (selectionOverlay != null && selectionOverlay.rects.isNotEmpty()) {
        res.overlayPaint.style = Paint.Style.FILL
        res.overlayPaint.color = selectionOverlay.highlightColorArgb
        res.overlayPaint.alpha = if (palette.isDark) 96 else 130
        val radius = 4f * density
        selectionOverlay.rects.forEach { rect ->
            canvas.drawRoundRect(rect, radius, radius, res.overlayPaint)
        }

        val handleColor = palette.accent.toArgb()
        res.handlePaint.color = handleColor
        res.handlePaint.style = Paint.Style.FILL
        val handleR = 6.5f * density

        selectionOverlay.startHandle?.let { p ->
            val first = selectionOverlay.rects.first()
            res.handlePaint.strokeWidth = 2f * density
            canvas.drawLine(first.left, first.top, first.left, first.bottom, res.handlePaint)
            canvas.drawCircle(p.x, p.y, handleR, res.handlePaint)
        }
        selectionOverlay.endHandle?.let { p ->
            val last = selectionOverlay.rects.last()
            res.handlePaint.strokeWidth = 2f * density
            canvas.drawLine(last.right, last.top, last.right, last.bottom, res.handlePaint)
            canvas.drawCircle(p.x, p.y, handleR, res.handlePaint)
        }
    }
}

/**
 * Exact Sutherland-Hodgman polygon half-plane clipper for fold line:
 * s(P) = P.x * nx + P.y * ny - mDotN
 */
private fun clipPolygonHalfPlane(
    polygon: List<PointF>,
    nx: Float,
    ny: Float,
    mDotN: Float,
    keepPositive: Boolean
): List<PointF> {
    if (polygon.isEmpty()) return emptyList()
    val output = ArrayList<PointF>(6)
    var prev = polygon.last()
    var prevDist = prev.x * nx + prev.y * ny - mDotN
    var prevInside = if (keepPositive) prevDist >= 0f else prevDist <= 0f

    for (curr in polygon) {
        val currDist = curr.x * nx + curr.y * ny - mDotN
        val currInside = if (keepPositive) currDist >= 0f else currDist <= 0f

        if (currInside != prevInside) {
            val denom = prevDist - currDist
            if (abs(denom) > 1e-5f) {
                val t = (prevDist / denom).coerceIn(0f, 1f)
                output.add(
                    PointF(
                        prev.x + t * (curr.x - prev.x),
                        prev.y + t * (curr.y - prev.y)
                    )
                )
            }
        }
        if (currInside) {
            output.add(curr)
        }
        prev = curr
        prevDist = currDist
        prevInside = currInside
    }
    return output
}

private fun buildPathFromPoly(path: Path, poly: List<PointF>) {
    path.reset()
    if (poly.isEmpty()) return
    path.moveTo(poly[0].x, poly[0].y)
    for (i in 1 until poly.size) {
        path.lineTo(poly[i].x, poly[i].y)
    }
    path.close()
}
