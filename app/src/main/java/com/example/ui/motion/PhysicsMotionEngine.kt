package com.example.ui.motion

import android.animation.ValueAnimator
import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import kotlinx.coroutines.launch

/**
 * CompositionLocal indicating whether reduced motion is preferred by the user or OS.
 * When true, spring overshoots, 3D tilts, and slide offsets settle gently or instantaneously.
 */
val LocalReducedMotion = compositionLocalOf { false }

/**
 * Inspects Android system animation settings to respect accessibility reduced-motion preferences.
 */
@Composable
fun rememberSystemReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        val animatorsEnabled = runCatching { ValueAnimator.areAnimatorsEnabled() }.getOrDefault(true)
        val scale = runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            )
        }.getOrDefault(1f)
        !animatorsEnabled || scale == 0f
    }
}

/**
 * Centralized physical spring specifications shared across the entire DoorCut interface.
 * Uses coherent mass/stiffness/damping relationships so buttons, cards, navigation pills,
 * sheets, and counters move as part of one unified physical digital material.
 */
@Stable
object DoorCutMotionSpecs {

    /** Snappy tactile response for button and card press/release compression. */
    fun <T> tactileSpring(reducedMotion: Boolean = false): FiniteAnimationSpec<T> =
        if (reducedMotion) tween(durationMillis = 90)
        else spring(
            dampingRatio = 0.68f,
            stiffness = Spring.StiffnessMediumLow
        )

    /** Fluid elastic spring for sliding navigation indicators, tab pills, and portal switches. */
    fun <T> liquidPillSpring(reducedMotion: Boolean = false): FiniteAnimationSpec<T> =
        if (reducedMotion) tween(durationMillis = 120)
        else spring(
            dampingRatio = 0.72f,
            stiffness = 420f
        )

    /** Smooth critically-damped settle for cards, sheets, and staggered content reveals. */
    fun <T> surfaceSettleSpring(reducedMotion: Boolean = false): FiniteAnimationSpec<T> =
        if (reducedMotion) tween(durationMillis = 110)
        else spring(
            dampingRatio = 0.82f,
            stiffness = 340f
        )

    /** Bouncy celebratory micro-spring for selection checkmarks, heart toggles, and badges. */
    fun <T> microBounceSpring(reducedMotion: Boolean = false): FiniteAnimationSpec<T> =
        if (reducedMotion) tween(durationMillis = 80)
        else spring(
            dampingRatio = 0.54f,
            stiffness = 520f
        )

    /** Directional step transition for multi-step booking flows (1 -> 2 -> 3 -> 4). */
    fun directionalStepTransform(
        forward: Boolean,
        reducedMotion: Boolean = false
    ): ContentTransform {
        if (reducedMotion) {
            return fadeIn(tween(120)) togetherWith fadeOut(tween(90))
        }
        val offsetSign = if (forward) 1 else -1
        val slideSpring = spring<IntOffset>(
            dampingRatio = 0.84f,
            stiffness = 380f
        )
        return (slideInHorizontally(animationSpec = slideSpring) { fullWidth ->
            (fullWidth * 0.18f * offsetSign).toInt()
        } + fadeIn(tween(180)) + scaleIn(
            initialScale = 0.97f,
            animationSpec = spring(dampingRatio = 0.84f, stiffness = 380f)
        )) togetherWith (slideOutHorizontally(animationSpec = slideSpring) { fullWidth ->
            (-fullWidth * 0.14f * offsetSign).toInt()
        } + fadeOut(tween(130)) + scaleOut(
            targetScale = 0.98f,
            animationSpec = tween(130)
        ))
    }

    /** Rolling numeric / summary counter transition for live service totals & counts. */
    fun rollingValueTransform(
        increasing: Boolean = true,
        reducedMotion: Boolean = false
    ): ContentTransform {
        if (reducedMotion) {
            return fadeIn(tween(90)) togetherWith fadeOut(tween(90))
        }
        val dir = if (increasing) 1 else -1
        return ContentTransform(
            targetContentEnter = slideInVertically(
                animationSpec = spring(dampingRatio = 0.76f, stiffness = 460f)
            ) { height -> (height * 0.65f * dir).toInt() } + fadeIn(tween(140)),
            initialContentExit = slideOutVertically(
                animationSpec = tween(120)
            ) { height -> (-height * 0.65f * dir).toInt() } + fadeOut(tween(110)),
            sizeTransform = SizeTransform(clip = false)
        )
    }
}

/**
 * Interactive tactile press & optical specular light physics modifier.
 *
 * Passively observes pointer down, drag coordinates, and release on [PointerEventPass.Initial]
 * WITHOUT consuming events, ensuring zero interference with `Button`, `Card(onClick)`,
 * `Modifier.clickable`, scrolling, or automated UI testing (`performClick()`).
 *
 * Features:
 * - Physical spring compression (`pressedScale`) with controlled elastic rebound
 * - Subtle touch-centered 3D perspective tilt (`maxTiltDegrees`)
 * - Touch-responsive radial optical specular highlight across the surface
 * - Subtle haptic tick on initial press when enabled
 */
fun Modifier.tactilePressPhysics(
    enabled: Boolean = true,
    pressedScale: Float = 0.972f,
    maxTiltDegrees: Float = 2.4f,
    specularColor: Color = Color(0xFFFFDF9E),
    specularAlpha: Float = 0.14f,
    enableHaptics: Boolean = false
): Modifier = composed {
    if (!enabled) return@composed this

    val reducedMotion = LocalReducedMotion.current
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    val scaleAnim = remember { Animatable(1f) }
    val tiltXAnim = remember { Animatable(0f) }
    val tiltYAnim = remember { Animatable(0f) }
    val glowAlphaAnim = remember { Animatable(0f) }

    var surfaceSize by remember { mutableStateOf(IntSize.Zero) }
    var pointerPosition by remember { mutableStateOf(Offset.Unspecified) }

    this
        .onSizeChanged { surfaceSize = it }
        .pointerInput(enabled, reducedMotion) {
            awaitEachGesture {
                var isDown = false
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val activeChange = event.changes.firstOrNull { it.pressed }
                    if (activeChange != null) {
                        pointerPosition = activeChange.position
                        if (!isDown) {
                            isDown = true
                            if (enableHaptics) {
                                runCatching {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            }
                            scope.launch {
                                scaleAnim.animateTo(
                                    targetValue = if (reducedMotion) 0.99f else pressedScale,
                                    animationSpec = spring(dampingRatio = 0.72f, stiffness = 600f)
                                )
                            }
                            scope.launch {
                                glowAlphaAnim.animateTo(
                                    targetValue = specularAlpha,
                                    animationSpec = tween(90)
                                )
                            }
                        }
                        if (!reducedMotion && surfaceSize.width > 0 && surfaceSize.height > 0) {
                            val normX = ((activeChange.position.x / surfaceSize.width) - 0.5f) * 2f
                            val normY = ((activeChange.position.y / surfaceSize.height) - 0.5f) * 2f
                            scope.launch {
                                tiltYAnim.snapTo((normX * maxTiltDegrees).coerceIn(-maxTiltDegrees, maxTiltDegrees))
                                tiltXAnim.snapTo((-normY * maxTiltDegrees).coerceIn(-maxTiltDegrees, maxTiltDegrees))
                            }
                        }
                    } else {
                        if (isDown) {
                            scope.launch {
                                scaleAnim.animateTo(
                                    targetValue = 1f,
                                    animationSpec = if (reducedMotion) tween(100)
                                    else spring(dampingRatio = 0.58f, stiffness = 440f)
                                )
                            }
                            scope.launch {
                                tiltXAnim.animateTo(0f, spring(dampingRatio = 0.75f, stiffness = 380f))
                            }
                            scope.launch {
                                tiltYAnim.animateTo(0f, spring(dampingRatio = 0.75f, stiffness = 380f))
                            }
                            scope.launch {
                                glowAlphaAnim.animateTo(0f, tween(220))
                            }
                        }
                        break
                    }
                }
            }
        }
        .graphicsLayer {
            scaleX = scaleAnim.value
            scaleY = scaleAnim.value
            rotationX = tiltXAnim.value
            rotationY = tiltYAnim.value
            cameraDistance = 14f * density.density
        }
        .drawWithContent {
            drawContent()
            val alpha = glowAlphaAnim.value
            if (alpha > 0.005f && pointerPosition != Offset.Unspecified) {
                val radius = maxOf(size.width, size.height) * 0.75f
                if (radius > 1f) {
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                specularColor.copy(alpha = alpha),
                                specularColor.copy(alpha = alpha * 0.35f),
                                Color.Transparent
                            ),
                            center = pointerPosition,
                            radius = radius
                        )
                    )
                }
            }
        }
}

/**
 * Staggered spring entrance modifier that settles a surface smoothly from a slight vertical offset.
 * Safely skips choreography when [LocalReducedMotion] is active.
 */
fun Modifier.staggeredSpringEntrance(
    index: Int = 0,
    initialOffsetY: Dp = 14.dp
): Modifier = composed {
    val reducedMotion = LocalReducedMotion.current
    if (reducedMotion) return@composed this

    val density = LocalDensity.current
    val initialPx = with(density) { initialOffsetY.toPx() }
    val progress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        val clampedDelayIndex = index.coerceIn(0, 5)
        if (clampedDelayIndex > 0) {
            kotlinx.coroutines.delay(clampedDelayIndex * 22L)
        }
        progress.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = 0.82f,
                stiffness = 360f
            )
        )
    }

    this.graphicsLayer {
        val p = progress.value
        alpha = (0.35f + 0.65f * p).coerceIn(0f, 1f)
        translationY = (1f - p) * initialPx
    }
}

/**
 * Physics-driven rolling text display for prices, durations, and item counts.
 */
@Composable
fun AnimatedSummaryText(
    text: String,
    numericOrderKey: Long = 0L,
    style: TextStyle,
    color: Color,
    fontWeight: FontWeight? = null,
    modifier: Modifier = Modifier
) {
    val reducedMotion = LocalReducedMotion.current
    var previousKey by remember { mutableStateOf(numericOrderKey) }
    val increasing = numericOrderKey >= previousKey
    LaunchedEffect(numericOrderKey) {
        previousKey = numericOrderKey
    }

    AnimatedContent(
        targetState = text,
        transitionSpec = {
            DoorCutMotionSpecs.rollingValueTransform(
                increasing = increasing,
                reducedMotion = reducedMotion
            )
        },
        label = "animated_summary_text",
        modifier = modifier
    ) { currentText ->
        Text(
            text = currentText,
            style = style,
            color = color,
            fontWeight = fontWeight
        )
    }
}
