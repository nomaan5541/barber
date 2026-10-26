package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ui.glass.DarkLiquidGlassPalette
import com.example.ui.glass.LightLiquidGlassPalette
import com.example.ui.glass.LocalLiquidGlassPalette
import com.example.ui.motion.LocalReducedMotion
import com.example.ui.motion.rememberSystemReducedMotion

private val DarkColorScheme = darkColorScheme(
    primary = BrassGold,
    onPrimary = OnBrassGoldDark,
    primaryContainer = BrassGoldContainerDark,
    onPrimaryContainer = BrassGoldLight,
    secondary = Color(0xFFE3C699),
    onSecondary = Color(0xFF3B2B13),
    secondaryContainer = Color(0xFF523E21),
    onSecondaryContainer = Color(0xFFFFE2B8),
    tertiary = SageVerifiedDark,
    onTertiary = Color(0xFF003823),
    tertiaryContainer = SageContainerDark,
    onTertiaryContainer = Color(0xFFA6F5CD),
    background = ObsidianBg,
    onBackground = Color(0xFFF5EFE6),
    surface = ObsidianSurface,
    onSurface = Color(0xFFF5EFE6),
    surfaceVariant = ObsidianSurfaceHigh,
    onSurfaceVariant = Color(0xFFD2C4B2),
    surfaceContainerLowest = ObsidianLowest,
    surfaceContainerLow = ObsidianSurfaceLow,
    surfaceContainer = ObsidianSurface,
    surfaceContainerHigh = ObsidianSurfaceHigh,
    surfaceContainerHighest = ObsidianSurfaceHighest,
    outline = ObsidianOutline,
    outlineVariant = ObsidianOutlineVariant,
    inverseSurface = Color(0xFFF5EFE6),
    inverseOnSurface = EspressoText,
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

private val LightColorScheme = lightColorScheme(
    primary = BrassGoldDark,
    onPrimary = Color.White,
    primaryContainer = BrassGoldContainerLight,
    onPrimaryContainer = Color(0xFF2B1C00),
    secondary = Color(0xFF645138),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF7E1C1),
    onSecondaryContainer = Color(0xFF231806),
    tertiary = SageVerified,
    onTertiary = Color.White,
    tertiaryContainer = SageContainer,
    onTertiaryContainer = Color(0xFF002113),
    background = ParchmentBg,
    onBackground = EspressoText,
    surface = ParchmentCard,
    onSurface = EspressoText,
    surfaceVariant = ParchmentSurface,
    onSurfaceVariant = MutedBronze,
    surfaceContainerLowest = AlabasterLowest,
    surfaceContainerLow = ParchmentSurfaceLow,
    surfaceContainer = ParchmentSurface,
    surfaceContainerHigh = ParchmentSurfaceHigh,
    surfaceContainerHighest = Color(0xFFDDD3C3),
    outline = ParchmentOutline,
    outlineVariant = ParchmentOutlineVariant,
    inverseSurface = EspressoHeaderBg,
    inverseOnSurface = Color(0xFFF8F3EC),
    error = CrimsonDispute,
    onError = Color.White,
    errorContainer = CrimsonContainer,
    onErrorContainer = Color(0xFF410E0B)
)

val DoorCutShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

@Composable
fun DoorCutTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val reducedMotion = rememberSystemReducedMotion()
    val glassPalette = if (darkTheme) DarkLiquidGlassPalette else LightLiquidGlassPalette

    CompositionLocalProvider(
        LocalReducedMotion provides reducedMotion,
        LocalLiquidGlassPalette provides glassPalette
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = DoorCutShapes,
            content = content
        )
    }
}
