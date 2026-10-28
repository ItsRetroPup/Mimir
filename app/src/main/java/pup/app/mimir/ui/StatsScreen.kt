package pup.app.mimir.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.text.DateFormat
import java.util.Date
import pup.app.mimir.R
import pup.app.mimir.domain.UsageStats

/**
 * The space savers' lifetime totals: space saved and games compressed on the left, and on the right
 * each tool's share, drawn with its own object, ink and block meter as home draws its blocks.
 */
@Composable
internal fun StatsScreen(stats: UsageStats, contentFocus: FocusRequester, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        if (maxWidth >= 700.dp) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Column(
                    Modifier
                        .weight(0.42f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 12.dp),
                ) { TotalsPanel(stats) }
                Column(
                    Modifier
                        .weight(0.58f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) { ToolBreakdown(stats, contentFocus) }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                TotalsPanel(stats)
                ToolBreakdown(stats, contentFocus)
            }
        }
    }
}

@Composable
private fun TotalsPanel(stats: UsageStats) {
    val palette = LocalMimirPalette.current
    val figure = MaterialTheme.typography.displaySmall.copy(fontFeatureSettings = "tnum")
    ConsolePanel(Modifier.fillMaxWidth(), contentPadding = PaddingValues(20.dp)) {
        Total(stringResource(R.string.stats_space_saved), formatBytes(stats.totalSpaceSavedBytes), figure)
        PanelRule()
        Total(stringResource(R.string.stats_games_compressed), "%,d".format(stats.totalGames), figure)
        PanelRule()
        if (stats.totalGames == 0) {
            Text(stringResource(R.string.stats_empty), style = MaterialTheme.typography.bodyMedium, color = palette.textMuted)
        }
        Text(
            stringResource(R.string.stats_since, DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(stats.sinceMillis))),
            style = MaterialTheme.typography.bodySmall,
            color = palette.textMuted,
        )
    }
}

@Composable
private fun Total(label: String, value: String, style: TextStyle) {
    val palette = LocalMimirPalette.current
    Column(
        Modifier.semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(label, style = MaterialTheme.typography.titleSmall, color = palette.textMuted)
        Text(value, style = style, color = palette.text, maxLines = 1)
    }
}

@Composable
private fun ToolBreakdown(stats: UsageStats, contentFocus: FocusRequester) {
    val palette = LocalMimirPalette.current
    // Meters are relative to the tool that saved the most, as size bars are to the largest file.
    val largestSaving = spaceSavers.maxOf { stats.usage(it.statsKey).spaceSavedBytes }.coerceAtLeast(1L)
    Text(
        stringResource(R.string.stats_by_tool),
        style = MaterialTheme.typography.headlineSmall,
        color = palette.text,
        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
    )
    spaceSavers.forEachIndexed { index, tool ->
        ToolStatsRow(
            tool = tool,
            stats = stats,
            largestSaving = largestSaving,
            modifier = if (index == 0) Modifier.focusRequester(contentFocus) else Modifier,
        )
    }
}

/** One tool as a data row; rows take focus so a controller can scroll the list. */
@Composable
private fun ToolStatsRow(tool: ToolEntry, stats: UsageStats, largestSaving: Long, modifier: Modifier = Modifier) {
    val palette = LocalMimirPalette.current
    val ink = tool.ink.resolve(palette)
    val usage = stats.usage(tool.statsKey)
    val used = usage.games > 0
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(shape)
            .background(palette.panel)
            .focusRing(shape)
            .focusable()
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SaveBlockArt(tool.art, if (used) ink else ink.copy(alpha = 0.55f), hole = palette.panel, modifier = Modifier.size(36.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                stringResource(tool.toolName),
                style = MaterialTheme.typography.titleMedium,
                color = if (used) palette.text else palette.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                if (used) pluralStringResource(R.plurals.stats_games, usage.games, usage.games) else stringResource(R.string.stats_not_used),
                style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                color = palette.textMuted,
            )
        }
        if (used) {
            val saved = usage.spaceSavedBytes.coerceAtLeast(0L)
            Column(Modifier.width(160.dp), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    stringResource(R.string.stats_saved_value, formatBytes(saved)),
                    style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                    color = if (saved > 0L) ink else palette.textMuted,
                    maxLines = 1,
                )
                BlockProgressBar(progress = saved.toFloat() / largestSaving, blocks = 16, height = 5.dp, ink = ink)
            }
        }
    }
}

private val ToolEntry.statsKey: String get() = UsageStats.keyFor(mode, converter)
