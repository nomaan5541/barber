package com.example.ui.glass

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.motion.DoorCutMotionSpecs
import com.example.ui.motion.LocalReducedMotion
import com.example.ui.motion.tactilePressPhysics
import com.example.ui.navigation.NavTabSpec
import com.example.ui.theme.BrassGold
import com.example.ui.theme.BrassGoldLight
import com.example.ui.theme.EspressoHeaderBg
import com.example.ui.theme.SageVerified

/**
 * Distinct optical material strengths for DoorCut's Living Liquid Glass system:
 * - [ChromeBar]: Translucent navigation & header bar with high optical clarity & crisp edge separation
 * - [AtelierCard]: Warm alabaster / obsidian layered card surface with Fresnel top-edge catchlight
 * - [FloatingPill]: Rich roasted-espresso optical capsule for sticky action bars & floating controls
 * - [HeroLens]: Translucent optical lens overlay over hero imagery with champagne brass rim illumination
 * - [AccentEmerald]: Botanical sage-tinted glass for verified trust states & live tracking cards
 */
enum class GlassMaterialStrength {
    ChromeBar,
    AtelierCard,
    FloatingPill,
    HeroLens,
    AccentEmerald
}

@Immutable
data class LiquidGlassPalette(
    val isDark: Boolean,
    val chromeBase: Color,
    val chromeHighlight: Color,
    val cardBase: Color,
    val cardInnerGlow: Color,
    val rimLightTop: Color,
    val rimLightBottom: Color,
    val ambientBrassOrb: Color,
    val ambientSageOrb: Color
)

val LightLiquidGlassPalette = LiquidGlassPalette(
    isDark = false,
    chromeBase = Color(0xEBFAF7F2),
    chromeHighlight = Color(0xFFFFFFFF),
    cardBase = Color(0xEEFFFFFF),
    cardInnerGlow = Color(0x66FFF8EC),
    rimLightTop = Color(0xE6FFFFFF),
    rimLightBottom = Color(0x66D5C6B0),
    ambientBrassOrb = Color(0x24E0B054),
    ambientSageOrb = Color(0x161D7A55)
)

val DarkLiquidGlassPalette = LiquidGlassPalette(
    isDark = true,
    chromeBase = Color(0xE6181411),
    chromeHighlight = Color(0x38FFE4B5),
    cardBase = Color(0xEB221D18),
    cardInnerGlow = Color(0x28E0B054),
    rimLightTop = Color(0x73FFE4B5),
    rimLightBottom = Color(0x334C4339),
    ambientBrassOrb = Color(0x26E0B054),
    ambientSageOrb = Color(0x1A6BD8AC)
)

val LocalLiquidGlassPalette = compositionLocalOf { LightLiquidGlassPalette }

/**
 * Subtle atmospheric atelier light backdrop rendered behind scrollable screens so
 * translucent Liquid Glass surfaces exhibit genuine optical depth and light transmission
 * while preserving 100% WCAG readability in both Light and Dark themes.
 */
@Composable
fun AmbientAtelierBackdrop(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val palette = LocalLiquidGlassPalette.current
    val bgColor = MaterialTheme.colorScheme.background

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
            .drawBehind {
                val w = size.width
                val h = size.height
                if (w > 0f && h > 0f) {
                    // Warm champagne brass upper-right ambient light pool
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(palette.ambientBrassOrb, Color.Transparent),
                            center = Offset(w * 0.88f, h * 0.10f),
                            radius = maxOf(w, h) * 0.52f
                        ),
                        radius = maxOf(w, h) * 0.52f,
                        center = Offset(w * 0.88f, h * 0.10f)
                    )
                    // Soft botanical emerald lower-left depth pool
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(palette.ambientSageOrb, Color.Transparent),
                            center = Offset(w * 0.12f, h * 0.72f),
                            radius = maxOf(w, h) * 0.48f
                        ),
                        radius = maxOf(w, h) * 0.48f,
                        center = Offset(w * 0.12f, h * 0.72f)
                    )
                }
            },
        content = content
    )
}

/**
 * Reusable Living Liquid Glass surface with:
 * - Calibrated translucent optical base
 * - Diagonal internal light transmission sheen
 * - Fresnel directional rim border (bright top-left optical catchlight, soft bottom-right shadow rim)
 * - Optional interactive tactile press & specular light tracking physics
 */
@Composable
fun LiquidGlassSurface(
    modifier: Modifier = Modifier,
    strength: GlassMaterialStrength = GlassMaterialStrength.AtelierCard,
    shape: Shape = RoundedCornerShape(22.dp),
    accentBorderColor: Color? = null,
    shadowElevation: Dp = 2.dp,
    onClick: (() -> Unit)? = null,
    interactivePhysics: Boolean = onClick != null,
    content: @Composable BoxScope.() -> Unit
) {
    val palette = LocalLiquidGlassPalette.current
    val dark = isSystemInDarkTheme()

    val (baseFill, topSheen, bottomSheen, rimStart, rimEnd, contentCol) = when (strength) {
        GlassMaterialStrength.ChromeBar -> GlassTokens(
            base = palette.chromeBase,
            sheenStart = palette.chromeHighlight.copy(alpha = if (dark) 0.08f else 0.55f),
            sheenEnd = Color.Transparent,
            rimStart = accentBorderColor ?: palette.rimLightTop,
            rimEnd = palette.rimLightBottom,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
        GlassMaterialStrength.AtelierCard -> GlassTokens(
            base = palette.cardBase,
            sheenStart = palette.cardInnerGlow,
            sheenEnd = Color.Transparent,
            rimStart = accentBorderColor ?: if (dark) BrassGold.copy(alpha = 0.34f) else palette.rimLightTop,
            rimEnd = accentBorderColor?.copy(alpha = 0.35f) ?: palette.rimLightBottom,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
        GlassMaterialStrength.FloatingPill -> GlassTokens(
            base = EspressoHeaderBg.copy(alpha = 0.94f),
            sheenStart = BrassGold.copy(alpha = 0.18f),
            sheenEnd = Color(0xFF14100D).copy(alpha = 0.35f),
            rimStart = accentBorderColor ?: BrassGoldLight.copy(alpha = 0.68f),
            rimEnd = BrassGold.copy(alpha = 0.25f),
            contentColor = Color(0xFFF8F3EC)
        )
        GlassMaterialStrength.HeroLens -> GlassTokens(
            base = EspressoHeaderBg.copy(alpha = 0.80f),
            sheenStart = Color.White.copy(alpha = 0.14f),
            sheenEnd = Color.Transparent,
            rimStart = accentBorderColor ?: BrassGoldLight.copy(alpha = 0.62f),
            rimEnd = BrassGold.copy(alpha = 0.22f),
            contentColor = Color.White
        )
        GlassMaterialStrength.AccentEmerald -> GlassTokens(
            base = if (dark) Color(0xEB132A20) else Color(0xF0F1FBF6),
            sheenStart = SageVerified.copy(alpha = 0.14f),
            sheenEnd = Color.Transparent,
            rimStart = accentBorderColor ?: SageVerified.copy(alpha = 0.62f),
            rimEnd = SageVerified.copy(alpha = 0.24f),
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    }

    val fresnelBorder = remember(rimStart, rimEnd) {
        BorderStroke(
            width = 1.dp,
            brush = Brush.linearGradient(
                colors = listOf(rimStart, rimEnd)
            )
        )
    }

    val physicsModifier = if (interactivePhysics) {
        Modifier.tactilePressPhysics(
            enabled = true,
            pressedScale = 0.976f,
            maxTiltDegrees = 2.0f,
            specularColor = if (strength == GlassMaterialStrength.FloatingPill || strength == GlassMaterialStrength.HeroLens) {
                BrassGoldLight
            } else {
                BrassGold
            },
            specularAlpha = 0.12f,
            enableHaptics = onClick != null
        )
    } else {
        Modifier
    }

    if (onClick != null) {
        Surface(
            onClick = onClick,
            shape = shape,
            color = baseFill,
            contentColor = contentCol,
            border = fresnelBorder,
            shadowElevation = shadowElevation,
            modifier = modifier.then(physicsModifier)
        ) {
            Box(
                modifier = Modifier.drawBehind {
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(topSheen, bottomSheen),
                            start = Offset.Zero,
                            end = Offset(size.width * 0.85f, size.height)
                        )
                    )
                },
                content = content
            )
        }
    } else {
        Surface(
            shape = shape,
            color = baseFill,
            contentColor = contentCol,
            border = fresnelBorder,
            shadowElevation = shadowElevation,
            modifier = modifier.then(physicsModifier)
        ) {
            Box(
                modifier = Modifier.drawBehind {
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(topSheen, bottomSheen),
                            start = Offset.Zero,
                            end = Offset(size.width * 0.85f, size.height)
                        )
                    )
                },
                content = content
            )
        }
    }
}

@Immutable
private data class GlassTokens(
    val base: Color,
    val sheenStart: Color,
    val sheenEnd: Color,
    val rimStart: Color,
    val rimEnd: Color,
    val contentColor: Color
)

/**
 * Finite state-reactive halo indicator for live booking/GPS statuses.
 * Triggers a smooth physical expansion settle whenever [statusKey] changes, without
 * running infinite animations that drain battery or block test synchronization.
 */
@Composable
fun LiveGlassPulseDot(
    statusKey: Any,
    color: Color = SageVerified,
    size: Dp = 10.dp,
    modifier: Modifier = Modifier
) {
    val reducedMotion = LocalReducedMotion.current
    val haloScale = remember { Animatable(1f) }
    val haloAlpha = remember { Animatable(0.32f) }

    LaunchedEffect(statusKey) {
        if (!reducedMotion) {
            haloScale.snapTo(0.85f)
            haloAlpha.snapTo(0.65f)
            haloScale.animateTo(
                targetValue = 1.45f,
                animationSpec = DoorCutMotionSpecs.microBounceSpring(false)
            )
            haloAlpha.animateTo(0.26f, tween(280))
        }
    }

    Box(
        modifier = modifier.size(size * 2.1f),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .graphicsLayer {
                    scaleX = haloScale.value
                    scaleY = haloScale.value
                    alpha = haloAlpha.value
                }
                .clip(CircleShape)
                .background(color)
        )
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(color)
        )
    }
}

/**
 * Living Liquid Glass Bottom Navigation Dock with a physical spring-sliding optical capsule
 * that glides smoothly between tabs, icon scale micro-bounce, haptic ticks, and full safe-area
 * `WindowInsets.navigationBars` support. Preserves every `tab.testTag` and 48dp+ touch target.
 */
@Composable
fun LiquidGlassNavigationDock(
    navTabs: List<NavTabSpec>,
    currentRoute: String,
    onSelectTab: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val reducedMotion = LocalReducedMotion.current
    val haptic = LocalHapticFeedback.current
    val palette = LocalLiquidGlassPalette.current
    val dark = palette.isDark

    val selectedIndex = remember(navTabs, currentRoute) {
        navTabs.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        LiquidGlassSurface(
            strength = GlassMaterialStrength.ChromeBar,
            shape = RoundedCornerShape(26.dp),
            shadowElevation = 8.dp,
            interactivePhysics = false,
            modifier = Modifier
                .widthIn(max = 620.dp)
                .fillMaxWidth()
                .height(68.dp)
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp, vertical = 6.dp)
            ) {
                val tabCount = navTabs.size.coerceAtLeast(1)
                val tabWidth = maxWidth / tabCount

                val pillOffsetX by animateDpAsState(
                    targetValue = tabWidth * selectedIndex,
                    animationSpec = DoorCutMotionSpecs.liquidPillSpring(reducedMotion),
                    label = "liquid_nav_pill_offset"
                )

                // Sliding Liquid Glass Active Pill Indicator
                Box(
                    modifier = Modifier
                        .offset(x = pillOffsetX)
                        .width(tabWidth)
                        .fillMaxHeight()
                        .padding(horizontal = 3.dp)
                        .shadow(
                            elevation = if (dark) 4.dp else 2.dp,
                            shape = RoundedCornerShape(20.dp),
                            ambientColor = BrassGold.copy(alpha = 0.25f),
                            spotColor = BrassGold.copy(alpha = 0.25f)
                        )
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.linearGradient(
                                colors = if (dark) {
                                    listOf(
                                        Color(0xFF3A2C16),
                                        Color(0xFF281F12)
                                    )
                                } else {
                                    listOf(
                                        EspressoHeaderBg,
                                        Color(0xFF332619)
                                    )
                                }
                            )
                        )
                        .drawBehind {
                            // Specular top highlight inside the active liquid capsule
                            drawRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        BrassGoldLight.copy(alpha = 0.26f),
                                        Color.Transparent
                                    ),
                                    startY = 0f,
                                    endY = size.height * 0.55f
                                )
                            )
                        }
                )

                // Interactive Tab Items Row
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    navTabs.forEachIndexed { index, tab ->
                        val isSelected = index == selectedIndex
                        val iconScale by animateFloatAsState(
                            targetValue = if (isSelected) 1.12f else 1.0f,
                            animationSpec = DoorCutMotionSpecs.microBounceSpring(reducedMotion),
                            label = "nav_icon_scale_$index"
                        )
                        val iconTint by animateColorAsState(
                            targetValue = if (isSelected) BrassGoldLight else MaterialTheme.colorScheme.onSurfaceVariant,
                            animationSpec = tween(180),
                            label = "nav_icon_tint_$index"
                        )
                        val labelColor by animateColorAsState(
                            targetValue = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            animationSpec = tween(180),
                            label = "nav_label_color_$index"
                        )

                        val interactionSource = remember { MutableInteractionSource() }
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(20.dp))
                                .semantics { this.selected = isSelected }
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = null,
                                    role = Role.Tab,
                                    onClick = {
                                        if (!isSelected) {
                                            runCatching {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            }
                                        }
                                        onSelectTab(tab.route)
                                    }
                                )
                                .testTag(tab.testTag),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = stringResource(tab.labelRes),
                                tint = iconTint,
                                modifier = Modifier
                                    .size(22.dp)
                                    .graphicsLayer {
                                        scaleX = iconScale
                                        scaleY = iconScale
                                    }
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = stringResource(tab.labelRes),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = labelColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
