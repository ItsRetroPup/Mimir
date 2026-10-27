package pup.app.mimir.ui

import androidx.compose.ui.graphics.lerp
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material3.RippleConfiguration
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Button
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import android.provider.Settings
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pup.app.mimir.R

/** The night field every screen sits on: a vertical fade with the memory card's slot grid drawn faintly over it. */
internal fun Modifier.consoleField(palette: MimirPalette): Modifier = drawBehind {
    drawRect(Brush.verticalGradient(listOf(palette.fieldTop, palette.fieldBottom)))
    val step = 48.dp.toPx()
    val line = palette.gridLine.copy(alpha = if (palette.isDark) 0.55f else 0.7f)
    var x = step
    while (x < size.width) {
        drawLine(line, start = androidx.compose.ui.geometry.Offset(x, 0f), end = androidx.compose.ui.geometry.Offset(x, size.height), strokeWidth = 1f)
        x += step
    }
    var y = step
    while (y < size.height) {
        drawLine(line, start = androidx.compose.ui.geometry.Offset(0f, y), end = androidx.compose.ui.geometry.Offset(size.width, y), strokeWidth = 1f)
        y += step
    }
}

/** A solid console panel with a thin cold edge light; [accent] tints the edge with a tool's ink. */
@Composable
internal fun ConsolePanel(
    modifier: Modifier = Modifier,
    accent: Color? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(12.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val palette = LocalMimirPalette.current
    val shape = MaterialTheme.shapes.medium
    Column(
        modifier = modifier
            .clip(shape)
            .background(palette.panel)
            .border(1.dp, (accent ?: palette.edge).copy(alpha = if (accent != null) 0.55f else 0.28f), shape)
            .padding(contentPadding),
        verticalArrangement = verticalArrangement,
        content = content,
    )
}

/** A face-button glyph as printed on the Thor's pad: a filled disc with its letter knocked out. */
@Composable
internal fun ButtonGlyph(letter: String, modifier: Modifier = Modifier, size: Dp = 22.dp, color: Color? = null) {
    val palette = LocalMimirPalette.current
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(color ?: palette.text),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            letter,
            color = palette.fieldBottom,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold, fontSize = (size.value * 0.55f).sp),
        )
    }
}

/** One entry in the legend bar: the face button that triggers it, and the touch target that does the same. */
internal data class LegendAction(
    val letter: String,
    val label: String,
    val enabled: Boolean = true,
    /** Primary actions render as filled targets so touch users find them too. */
    val emphasised: Boolean = false,
    val onClick: (() -> Unit)? = null,
)

/** The bar along the foot of every screen, like a console's button legend. */
@Composable
internal fun LegendBar(
    actions: List<LegendAction>,
    modifier: Modifier = Modifier,
    leading: @Composable RowScope.() -> Unit = {},
) {
    val palette = LocalMimirPalette.current
    Surface(color = palette.panel.copy(alpha = 0.94f), modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 2.dp)
                .heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, content = leading)
            actions.forEach { LegendItem(it) }
        }
    }
}

@Composable
private fun LegendItem(action: LegendAction) {
    val palette = LocalMimirPalette.current
    val ink = MaterialTheme.colorScheme.primary
    val alpha = if (action.enabled) 1f else 0.38f
    val shape = RoundedCornerShape(50)
    val clickable = action.onClick != null
    Row(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clip(shape)
            .then(
                if (action.emphasised) Modifier.background(ink.copy(alpha = 0.18f * alpha)).border(1.dp, ink.copy(alpha = 0.6f * alpha), shape)
                else Modifier,
            )
            .then(
                if (clickable) Modifier.focusRing(shape).clickable(enabled = action.enabled, role = Role.Button) { action.onClick?.invoke() }
                else Modifier,
            )
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ButtonGlyph(action.letter, color = (if (action.emphasised) ink else palette.text).copy(alpha = alpha))
        Text(
            action.label,
            style = MaterialTheme.typography.labelLarge,
            color = (if (action.emphasised) ink else palette.text).copy(alpha = alpha),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** A status line for the current tool; errors take the error colour and an error icon. */
@Composable
internal fun StatusLine(message: StatusMessage, modifier: Modifier = Modifier) {
    val palette = LocalMimirPalette.current
    // Amber stays reserved for destructive confirmation; failures use the error role.
    val tint = if (message.isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(tint.copy(alpha = 0.12f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            if (message.isError) Icons.Outlined.ErrorOutline else Icons.Outlined.Info,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp),
        )
        Text(message.text, style = MaterialTheme.typography.bodyMedium, color = palette.text)
    }
}

/** True when the user has turned animations off; signature motion parks instead of looping. */
@Composable
internal fun reduceMotion(): Boolean {
    val resolver = LocalContext.current.contentResolver
    return remember(resolver) {
        Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}

/**
 * Lifts an element while it holds focus and lets it settle with a small bounce, the way a save
 * block rises when the cursor reaches it. [onFocus] reports focus changes to the caller.
 */
internal fun Modifier.focusLift(onFocus: (Boolean) -> Unit = {}): Modifier = composed {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (focused) 1.06f else 1f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMediumLow),
        label = "focusLift",
    )
    this
        .onFocusChanged {
            focused = it.hasFocus
            onFocus(it.hasFocus)
        }
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            translationY = -(scale - 1f) * 60f
        }
}

/** Plain-language empty state for a pane, with the button that gets the user moving. */
@Composable
internal fun EmptyPane(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    art: @Composable () -> Unit = {},
) {
    val palette = LocalMimirPalette.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        art()
        Text(title, style = MaterialTheme.typography.headlineSmall, color = palette.text)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = palette.textMuted)
    }
}

/** A thin rule in the panel's grid colour. */
@Composable
internal fun PanelRule(modifier: Modifier = Modifier) {
    val palette = LocalMimirPalette.current
    Box(
        modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .size(height = 1.dp, width = 0.dp)
            .background(palette.gridLine),
    )
}

/**
 * Mimir's logo. The artwork is white on transparent, so on the daylight palette it sits on a
 * night-coloured tile to stay visible.
 */
@Composable
internal fun MimirMark(modifier: Modifier = Modifier, contentDescription: String? = null) {
    val palette = LocalMimirPalette.current
    Box(
        modifier
            .clip(RoundedCornerShape(22))
            .background(if (palette.isDark) Color.Transparent else palette.text),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painterResource(R.drawable.mimir_launcher_m),
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

/** Pressing still ripples; focus is shown by brightening the fill and an offset ring, never by dimming it. */
@OptIn(ExperimentalMaterial3Api::class)
private val InkButtonRipple = RippleConfiguration(
    rippleAlpha = RippleAlpha(draggedAlpha = 0.16f, focusedAlpha = 0f, hoveredAlpha = 0.08f, pressedAlpha = 0.16f),
)

/**
 * A filled button in the page's ink (or [containerColor]). Material's focus overlay draws the
 * dark content colour over the fill, which reads as disabled on a handheld; this lifts the fill
 * and rings it instead.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun InkButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    content: @Composable RowScope.() -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    // On the night palette the label is dark, so a lighter fill keeps contrast; in daylight the
    // label is white and lightening would fail it, so focus there is the ring and lift alone.
    val brighten = focused && LocalMimirPalette.current.isDark
    CompositionLocalProvider(LocalRippleConfiguration provides InkButtonRipple) {
        Button(
            onClick = onClick,
            enabled = enabled,
            interactionSource = source,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (brighten) lerp(containerColor, Color.White, 0.22f) else containerColor,
                contentColor = contentColor,
            ),
            modifier = modifier
                .focusLift()
                .focusRing()
                .padding(4.dp),
            content = content,
        )
    }
}
