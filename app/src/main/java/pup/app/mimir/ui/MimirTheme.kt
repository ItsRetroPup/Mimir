package pup.app.mimir.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RippleConfiguration
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MimirTheme(
    useDarkMode: Boolean,
    content: @Composable () -> Unit,
) {
    val lightScheme = lightColorScheme(
        primary = Color(0xFF7B1FA2),
        onPrimary = Color.White,
        secondary = Color(0xFF6A1B9A),
        onSecondary = Color.White,
        tertiary = Color(0xFF5F5663),
        background = Color(0xFFFAF7FC),
        onBackground = Color(0xFF1A111F),
        surface = Color(0xFFFFFFFF),
        onSurface = Color(0xFF1A111F),
        surfaceVariant = Color(0xFFEEE5F2),
        outline = Color(0xFF76677A),
    )
    val darkScheme = darkColorScheme(
        primary = Color(0xFFE0B0FF),
        onPrimary = Color(0xFF2A003F),
        secondary = Color(0xFFF5B7FF),
        onSecondary = Color(0xFF24102F),
        tertiary = Color(0xFFD0C4D8),
        background = Color(0xFF100B14),
        onBackground = Color(0xFFF5EDF8),
        surface = Color(0xFF19121F),
        onSurface = Color(0xFFF5EDF8),
        surfaceVariant = Color(0xFF30243A),
        outline = Color(0xFFA99AAF),
    )

    val typography = MaterialTheme.typography.copy(
        displaySmall = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontSize = 28.sp,
            lineHeight = 36.sp,
            fontWeight = FontWeight.SemiBold,
        ),
        headlineMedium = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontSize = 24.sp,
            lineHeight = 32.sp,
            fontWeight = FontWeight.SemiBold,
        ),
        titleLarge = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontSize = 20.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.SemiBold,
        ),
        titleMedium = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontSize = 18.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.SemiBold,
        ),
        bodyLarge = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontSize = 18.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.Normal,
        ),
        bodyMedium = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.Normal,
        ),
        bodySmall = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Normal,
        ),
        labelLarge = TextStyle(
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.8.sp,
        ),
        labelMedium = TextStyle(
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.0.sp,
        ),
    )

    MaterialTheme(
        colorScheme = if (useDarkMode) darkScheme else lightScheme,
        typography = typography,
        shapes = Shapes(
            extraSmall = RoundedCornerShape(10.dp),
            small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(16.dp),
            large = RoundedCornerShape(16.dp),
        ),
    ) {
        // Controller and keyboard focus must be obvious on a handheld; Material's default focus
        // layer is barely visible, so every ripple-based control gets a much stronger one.
        CompositionLocalProvider(
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
