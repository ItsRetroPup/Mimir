package pup.app.mimir.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/** A row of mutually exclusive options as filter chips, in the page's ink. */
@Composable
@OptIn(ExperimentalLayoutApi::class)
internal fun <T> ChoiceRow(
    options: Iterable<T>,
    selected: T,
    enabled: Boolean,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
) {
    val palette = LocalMimirPalette.current
    val ink = MaterialTheme.colorScheme.primary
    // Moving into the row with a controller lands on the current choice, not the nearest chip.
    val selectedFocus = remember { FocusRequester() }
    FlowRow(
        modifier = Modifier.enterFocusAt(selectedFocus),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                enabled = enabled,
                label = { Text(label(option), style = MaterialTheme.typography.labelLarge) },
                shape = RoundedCornerShape(50),
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = palette.panelRaised,
                    labelColor = palette.text,
                    selectedContainerColor = ink,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                ),
                modifier = if (option == selected) Modifier.focusRequester(selectedFocus) else Modifier,
            )
        }
    }
}

/** Groups a row of controls so directional focus entering it always lands on [target]. */
@OptIn(ExperimentalComposeUiApi::class)
internal fun Modifier.enterFocusAt(target: FocusRequester): Modifier =
    focusProperties { enter = { target } }.focusGroup()

/**
 * Outlines an element while it, or a child, holds keyboard or controller focus. Material's own
 * focus state layer is too faint to follow on a handheld screen.
 */
internal fun Modifier.focusRing(shape: Shape = RoundedCornerShape(50)): Modifier = composed {
    var focused by remember { mutableStateOf(false) }
    this
        .onFocusChanged { focused = it.hasFocus }
        .border(
            width = 3.dp,
            color = if (focused) MaterialTheme.colorScheme.primary else Color.Transparent,
            shape = shape,
        )
}
