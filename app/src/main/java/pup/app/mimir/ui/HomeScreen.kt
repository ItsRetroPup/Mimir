package pup.app.mimir.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Coffee
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderZip
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Subscriptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import pup.app.mimir.R
import pup.app.mimir.domain.ConverterTool
import pup.app.mimir.domain.ToolMode

internal enum class AppSection {
    Home,
    EsDeSystems,
    Zipper,
    Organizer,
    ChdMan,
    Rvz,
    Zcci,
    Nsz,
    Xiso,
    Vita,
}

internal data class ToolEntry(
    @StringRes val goal: Int,
    @StringRes val toolName: Int,
    @StringRes val description: Int,
    val mode: ToolMode,
    val section: AppSection,
    val converter: ConverterTool? = null,
    val icon: ImageVector,
)

internal val utilities = listOf(
    ToolEntry(R.string.goal_fix_multidisc, R.string.tool_multidisc, R.string.tool_multidisc_description, ToolMode.MultiDiscOrganizer, AppSection.Organizer, icon = Icons.Outlined.Archive),
    ToolEntry(R.string.goal_create_vita_shortcuts, R.string.tool_vita_shortcuts, R.string.tool_vita_shortcuts_description, ToolMode.VitaAppIds, AppSection.Vita, icon = Icons.Outlined.Home),
    ToolEntry(R.string.goal_setup_esde, R.string.tool_esde_systems, R.string.tool_esde_systems_description, ToolMode.EsDeSystems, AppSection.EsDeSystems, icon = Icons.Outlined.Folder),
    ToolEntry(R.string.goal_convert_switch_packages, R.string.tool_nsz_to_nsp, R.string.tool_nsz_to_nsp_description, ToolMode.Converter, AppSection.Nsz, ConverterTool.NszNsp, Icons.Outlined.Archive),
)

internal val spaceSavers = listOf(
    ToolEntry(R.string.goal_compress_cartridge_roms, R.string.tool_romzipper, R.string.tool_romzipper_description, ToolMode.RomZipper, AppSection.Zipper, icon = Icons.Outlined.FolderZip),
    ToolEntry(R.string.goal_compress_disc_images, R.string.tool_chdman, R.string.tool_chdman_description, ToolMode.Converter, AppSection.ChdMan, ConverterTool.Chd, Icons.Outlined.Archive),
    ToolEntry(R.string.goal_shrink_gamecube_wii, R.string.tool_dolphin_rvz, R.string.tool_dolphin_rvz_description, ToolMode.Converter, AppSection.Rvz, ConverterTool.DolphinRvz, Icons.Outlined.Archive),
    ToolEntry(R.string.goal_shrink_3ds, R.string.tool_azahar_zcci, R.string.tool_azahar_zcci_description, ToolMode.Converter, AppSection.Zcci, ConverterTool.AzaharZcci, Icons.Outlined.Archive),
    ToolEntry(R.string.goal_shrink_xbox, R.string.tool_xbox_xiso, R.string.tool_xbox_xiso_description, ToolMode.Converter, AppSection.Xiso, ConverterTool.XboxXiso, Icons.Outlined.Archive),
)

/** The tool shown in [section], or null for the home screen. */
internal fun toolFor(section: AppSection): ToolEntry? = (utilities + spaceSavers).firstOrNull { it.section == section }

@Composable
internal fun SupportSection(
    onOpenYoutube: () -> Unit,
    onOpenKofi: () -> Unit,
    onOpenBuyMeACoffee: () -> Unit,
) {
    StyledCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Label(stringResource(R.string.support_title))
            Text(
                stringResource(R.string.support_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SupportLinkButton("YouTube", Icons.Outlined.Subscriptions, onOpenYoutube)
                SupportLinkButton("Ko-fi", Icons.Outlined.Favorite, onOpenKofi)
                SupportLinkButton("Buy Me a Coffee", Icons.Outlined.Coffee, onOpenBuyMeACoffee)
            }
        }
    }
}

@Composable
internal fun SupportLinkButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    TextButton(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
    ) {
        Icon(imageVector = icon, contentDescription = label)
        Spacer(Modifier.size(6.dp))
        Text(label, maxLines = 1)
    }
}

@Composable
internal fun HomeIntroSection() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(R.string.app_name),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            stringResource(R.string.home_intro),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f),
        )
    }
}

@Composable
internal fun ToolPageHeader(tool: ToolEntry) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stringResource(tool.toolName), style = MaterialTheme.typography.headlineMedium)
        Text(
            stringResource(tool.description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
        )
    }
}

@Composable
internal fun HomeFooter() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            stringResource(R.string.home_footer),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.64f),
        )
    }
}

@Composable
internal fun ToolModeCards(
    selectedMode: ToolMode,
    selectedConverterTool: ConverterTool,
    onPageSelected: (ToolEntry) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.home_prompt), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.home_prepare_library), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        utilities.forEach { tool ->
            ToolListItem(
                title = stringResource(tool.goal),
                description = stringResource(R.string.tool_card_description, stringResource(tool.toolName), stringResource(tool.description)),
                icon = tool.icon,
                selected = selectedMode == tool.mode && (tool.converter == null || selectedConverterTool == tool.converter),
                onClick = { onPageSelected(tool) },
            )
        }
        Text(stringResource(R.string.home_make_room), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        spaceSavers.forEach { tool ->
            ToolListItem(
                title = stringResource(tool.goal),
                description = stringResource(R.string.tool_card_description, stringResource(tool.toolName), stringResource(tool.description)),
                icon = tool.icon,
                selected = selectedMode == tool.mode && (tool.converter == null || selectedConverterTool == tool.converter),
                onClick = { onPageSelected(tool) },
            )
        }
    }
}

@Composable
internal fun ToolListItem(
    title: String,
    description: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            } else {
                MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
            },
        ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = if (selected) 0.9f else 0.5f),
        ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
                )
            }
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = stringResource(R.string.open_tool, title),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

internal const val YOUTUBE_CHANNEL_URL = "https://youtube.com/@ItsRetroPup"
internal const val KOFI_URL = "https://ko-fi.com/retropup84752"
internal const val BUY_ME_A_COFFEE_URL = "https://buymeacoffee.com/retropupz"
