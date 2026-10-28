package pup.app.mimir.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RippleConfiguration
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import pup.app.mimir.R

/**
 * Colours of the memory-card world that Material's colour roles do not name: the night field the
 * slots sit on, its gridlines, the cold edge light on panels, and the amber kept for destructive
 * confirmation.
 */
@Immutable
data class MimirPalette(
    val fieldTop: Color,
    val fieldBottom: Color,
    val gridLine: Color,
    val panel: Color,
    val panelRaised: Color,
    val edge: Color,
    val text: Color,
    val textMuted: Color,
    val caution: Color,
    val onCaution: Color,
    val success: Color,
    val isDark: Boolean,
)

internal val LocalMimirPalette = staticCompositionLocalOf { NightPalette }

private val NightPalette = MimirPalette(
    fieldTop = Color(0xFF0B1024),
    fieldBottom = Color(0xFF05070D),
    gridLine = Color(0xFF1B2650),
    panel = Color(0xFF111934),
    panelRaised = Color(0xFF18224A),
    edge = Color(0xFF7FA7FF),
    text = Color(0xFFE8EEFF),
    textMuted = Color(0xFF9AA7CB),
    caution = Color(0xFFFFB347),
    onCaution = Color(0xFF2A1600),
    success = Color(0xFF6BE3A4),
    isDark = true,
)

private val DaylightPalette = MimirPalette(
    fieldTop = Color(0xFFE6ECF8),
    fieldBottom = Color(0xFFD3DBEC),
    gridLine = Color(0xFFBCC8E2),
    panel = Color(0xFFF7F9FD),
    panelRaised = Color(0xFFFFFFFF),
    edge = Color(0xFF3550C8),
    text = Color(0xFF0C1430),
    textMuted = Color(0xFF475478),
    caution = Color(0xFFA85200),
    onCaution = Color(0xFFFFFFFF),
    success = Color(0xFF0E7A4A),
    isDark = false,
)

@OptIn(ExperimentalTextApi::class)
private val Rubik = FontFamily(
    listOf(400, 500, 600, 700, 800).map { weight ->
        Font(
            resId = R.font.rubik,
            weight = FontWeight(weight),
            variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
        )
    },
)

private fun rubik(size: Int, line: Int, weight: Int, tracking: Double = 0.0) = TextStyle(
    fontFamily = Rubik,
    fontSize = size.sp,
    lineHeight = line.sp,
    fontWeight = FontWeight(weight),
    letterSpacing = tracking.em,
)

/** A tight scale: rank comes from weight and reversal more than from size jumps. */
private val MimirTypography = Typography(
    displayLarge = rubik(44, 48, 800, -0.02),
    displayMedium = rubik(38, 42, 800, -0.02),
    displaySmall = rubik(32, 36, 800, -0.015),
    headlineLarge = rubik(28, 32, 700, -0.01),
    headlineMedium = rubik(24, 28, 700, -0.01),
    headlineSmall = rubik(21, 26, 700),
    titleLarge = rubik(19, 24, 600),
    titleMedium = rubik(16, 21, 600),
    titleSmall = rubik(14, 18, 600),
    bodyLarge = rubik(16, 23, 400),
    bodyMedium = rubik(14, 20, 400),
    bodySmall = rubik(12, 17, 400),
    labelLarge = rubik(14, 18, 600, 0.01),
    labelMedium = rubik(12, 16, 600, 0.02),
    labelSmall = rubik(11, 14, 600, 0.04),
)

private fun schemeFor(palette: MimirPalette): ColorScheme {
    val base = if (palette.isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = palette.edge,
        onPrimary = palette.fieldBottom,
        primaryContainer = palette.panelRaised,
        onPrimaryContainer = palette.text,
        secondary = palette.edge,
        onSecondary = palette.fieldBottom,
        secondaryContainer = palette.panelRaised,
        onSecondaryContainer = palette.text,
        tertiary = palette.caution,
        onTertiary = palette.onCaution,
        background = palette.fieldBottom,
        onBackground = palette.text,
        surface = palette.panel,
        onSurface = palette.text,
        surfaceVariant = palette.panelRaised,
        onSurfaceVariant = palette.textMuted,
        surfaceContainerLowest = palette.panel,
        surfaceContainerLow = palette.panel,
        surfaceContainer = palette.panel,
        surfaceContainerHigh = palette.panelRaised,
        surfaceContainerHighest = palette.panelRaised,
        outline = palette.edge.copy(alpha = 0.55f),
        outlineVariant = palette.gridLine,
        error = if (palette.isDark) Color(0xFFFF7A7A) else Color(0xFFC62828),
        onError = palette.fieldBottom,
    )
}

/**
 * Re-themes Material's primary roles with a tool's ink, so every button, chip, checkbox and focus
 * highlight on that tool's page speaks in its colour.
 */
@Composable
internal fun InkTheme(ink: Color, content: @Composable () -> Unit) {
    val palette = LocalMimirPalette.current
    val onInk = if (palette.isDark) palette.fieldBottom else Color.White
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
            primary = ink,
            onPrimary = onInk,
            secondary = ink,
            onSecondary = onInk,
            outline = ink.copy(alpha = 0.6f),
        ),
        typography = MaterialTheme.typography,
        shapes = MaterialTheme.shapes,
        content = content,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MimirTheme(
    useDarkMode: Boolean,
    content: @Composable () -> Unit,
) {
    val palette = if (useDarkMode) NightPalette else DaylightPalette
    MaterialTheme(
        colorScheme = schemeFor(palette),
        typography = MimirTypography,
        shapes = Shapes(
            extraSmall = RoundedCornerShape(8.dp),
            small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(16.dp),
            large = RoundedCornerShape(20.dp),
            extraLarge = RoundedCornerShape(24.dp),
        ),
    ) {
        // Controller and keyboard focus must be obvious on a handheld; Material's default focus
        // layer is barely visible, so every ripple-based control gets a much stronger one.
        CompositionLocalProvider(
            LocalMimirPalette provides palette,
            LocalRippleConfiguration provides RippleConfiguration(rippleAlpha = FocusRippleAlpha),
            content = content,
        )
    }
}

private val FocusRippleAlpha = RippleAlpha(
    draggedAlpha = 0.16f,
    focusedAlpha = 0.32f,
    hoveredAlpha = 0.08f,
    pressedAlpha = 0.16f,
)
