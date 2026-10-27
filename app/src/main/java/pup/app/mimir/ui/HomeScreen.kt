package pup.app.mimir.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
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
    val art: ToolArt,
    val ink: ToolInk,
    /** Formats the tool reads and writes, shown untranslated in the info panel. */
    val reads: String,
    val writes: String,
)

internal val utilities = listOf(
    ToolEntry(
        R.string.goal_fix_multidisc, R.string.tool_multidisc, R.string.tool_multidisc_description,
        ToolMode.MultiDiscOrganizer, AppSection.Organizer, art = ToolArt.DiscStack,
        ink = ToolInk(Color(0xFFFF8A3D), Color(0xFFC2520F)), reads = ".cue  .chd  .iso", writes = ".m3u + folders",
    ),
    ToolEntry(
        R.string.goal_create_vita_shortcuts, R.string.tool_vita_shortcuts, R.string.tool_vita_shortcuts_description,
        ToolMode.VitaAppIds, AppSection.Vita, art = ToolArt.VitaCard,
        ink = ToolInk(Color(0xFF5B95FF), Color(0xFF2558D0)), reads = "Title IDs", writes = ".psvita  .dpt",
    ),
    ToolEntry(
        R.string.goal_setup_esde, R.string.tool_esde_systems, R.string.tool_esde_systems_description,
        ToolMode.EsDeSystems, AppSection.EsDeSystems, art = ToolArt.Folders,
        ink = ToolInk(Color(0xFF34D399), Color(0xFF087A53)), reads = "es_systems.xml", writes = "custom_systems",
    ),
    ToolEntry(
        R.string.goal_convert_switch_packages, R.string.tool_nsz_to_nsp, R.string.tool_nsz_to_nsp_description,
        ToolMode.Converter, AppSection.Nsz, ConverterTool.NszNsp, art = ToolArt.SwitchCart,
        ink = ToolInk(Color(0xFFFF5C93), Color(0xFFC81E5E)), reads = ".nsz", writes = ".nsp",
    ),
)

internal val spaceSavers = listOf(
    ToolEntry(
        R.string.goal_compress_cartridge_roms, R.string.tool_romzipper, R.string.tool_romzipper_description,
        ToolMode.RomZipper, AppSection.Zipper, art = ToolArt.ZipCart,
        ink = ToolInk(Color(0xFFFFC93C), Color(0xFF9A6B00)), reads = ".gba  .sfc  .nes …", writes = ".zip",
    ),
    ToolEntry(
        R.string.goal_compress_disc_images, R.string.tool_chdman, R.string.tool_chdman_description,
        ToolMode.Converter, AppSection.ChdMan, ConverterTool.Chd, art = ToolArt.PackedDisc,
        ink = ToolInk(Color(0xFF2DD4E6), Color(0xFF0B7F8E)), reads = ".cue  .gdi  .iso", writes = ".chd",
    ),
    ToolEntry(
        R.string.goal_shrink_gamecube_wii, R.string.tool_dolphin_rvz, R.string.tool_dolphin_rvz_description,
        ToolMode.Converter, AppSection.Rvz, ConverterTool.DolphinRvz, art = ToolArt.MiniDisc,
        ink = ToolInk(Color(0xFFB197FC), Color(0xFF6A47D6)), reads = ".iso", writes = ".rvz",
    ),
    ToolEntry(
        R.string.goal_shrink_3ds, R.string.tool_azahar_zcci, R.string.tool_azahar_zcci_description,
        ToolMode.Converter, AppSection.Zcci, ConverterTool.AzaharZcci, art = ToolArt.HandheldCart,
        ink = ToolInk(Color(0xFFB5E853), Color(0xFF55800B)), reads = ".3ds  .cci", writes = ".zcci",
    ),
    ToolEntry(
        R.string.goal_shrink_xbox, R.string.tool_xbox_xiso, R.string.tool_xbox_xiso_description,
        ToolMode.Converter, AppSection.Xiso, ConverterTool.XboxXiso, art = ToolArt.TrimmedDisc,
        ink = ToolInk(Color(0xFF4ADE80), Color(0xFF13803B)), reads = "Redump .iso", writes = "XISO .iso",
    ),
)

/** The tool shown in [section], or null for the home screen. */
internal fun toolFor(section: AppSection): ToolEntry? = (utilities + spaceSavers).firstOrNull { it.section == section }

private const val SLOT_CELLS = 5
private val SLOT_GAP = 14.dp
private val SLOT_TITLE = 24.dp
private val SLOT_INNER_GAP = 6.dp
private val BLOCK_LABEL = 36.dp

/**
 * Home as a memory card manager: two slots of save blocks on the left, and on the right the info
 * panel for whichever block the cursor rests on. About Mimir lives in the footer, not on the card.
 */
@Composable
internal fun HomeScreen(
    contentFocus: FocusRequester,
    returnSection: AppSection?,
    returnFocus: FocusRequester,
    onOpen: (ToolEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    var focusedSection by rememberSaveable { mutableStateOf<AppSection?>(null) }
    val focusedTool = focusedSection?.let(::toolFor)
    BoxWithConstraints(modifier.fillMaxSize()) {
        val wide = maxWidth >= 600.dp
        val slots: @Composable (Modifier, Dp) -> Unit = { slotModifier, cellMax ->
            Column(
                modifier = slotModifier
                    .focusRequester(contentFocus)
                    .focusGroup(),
                verticalArrangement = Arrangement.spacedBy(SLOT_GAP),
            ) {
                MemorySlot(
                    title = stringResource(R.string.slot_utilities),
                    tools = utilities,
                    cellMax = cellMax,
                    returnSection = returnSection,
                    returnFocus = returnFocus,
                    focusedSection = focusedSection,
                    onFocus = { focusedSection = it },
                    onOpen = onOpen,
                )
                MemorySlot(
                    title = stringResource(R.string.slot_space_savers),
                    tools = spaceSavers,
                    cellMax = cellMax,
                    returnSection = returnSection,
                    returnFocus = returnFocus,
                    focusedSection = focusedSection,
                    onFocus = { focusedSection = it },
                    onOpen = onOpen,
                )
            }
        }
        val info: @Composable (Modifier) -> Unit = { infoModifier ->
            if (focusedTool != null) {
                ToolInfoPanel(focusedTool, infoModifier, fillHeight = wide)
            } else {
                WelcomePanel(infoModifier)
            }
        }
        if (wide) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                BoxWithConstraints(
                    Modifier
                        .weight(0.6f)
                        .fillMaxHeight(),
                ) {
                    // Both slots fit the screen without scrolling: size cells from the height too.
                    val byHeight = (maxHeight - SLOT_GAP - (SLOT_TITLE + SLOT_INNER_GAP + BLOCK_LABEL) * 2) / 2
                    slots(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), byHeight)
                }
                info(
                    Modifier
                        .weight(0.4f)
                        .fillMaxHeight(),
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                slots(Modifier.fillMaxWidth(), Dp.Infinity)
                info(Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun MemorySlot(
    title: String,
    tools: List<ToolEntry>,
    cellMax: Dp,
    returnSection: AppSection?,
    returnFocus: FocusRequester,
    focusedSection: AppSection?,
    onFocus: (AppSection) -> Unit,
    onOpen: (ToolEntry) -> Unit,
) {
    val palette = LocalMimirPalette.current
    val used = tools.size
    Column(verticalArrangement = Arrangement.spacedBy(SLOT_INNER_GAP)) {
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            color = palette.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.height(SLOT_TITLE),
        )
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val gap = 10.dp
            val cell = minOf((maxWidth - gap * (SLOT_CELLS - 1)) / SLOT_CELLS, cellMax)
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                tools.forEach { tool ->
                    SaveBlock(
                        tool = tool,
                        size = cell,
                        selected = focusedSection == tool.section,
                        modifier = if (tool.section == returnSection) Modifier.focusRequester(returnFocus) else Modifier,
                        onFocus = { onFocus(tool.section) },
                        onClick = { onOpen(tool) },
                    )
                }
                repeat(SLOT_CELLS - used) { EmptyCell(cell) }
            }
        }
    }
}

/** One tool on the memory card: its drawn object in a slot cell, its name beneath. */
@Composable
private fun SaveBlock(
    tool: ToolEntry,
    size: Dp,
    selected: Boolean,
    onFocus: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalMimirPalette.current
    val ink = tool.ink.resolve(palette)
    var focused by remember { mutableStateOf(false) }
    val name = stringResource(tool.toolName)
    BlockCell(
        size = size,
        label = name,
        ink = ink,
        focused = focused,
        selected = selected,
        modifier = modifier.focusLift { hasFocus ->
            focused = hasFocus
            if (hasFocus) onFocus()
        },
        onClick = onClick,
        contentDescription = stringResource(R.string.open_tool, name),
    ) {
        SaveBlockArt(tool.art, ink, hole = palette.panel, modifier = Modifier.fillMaxSize(), turning = focused)
    }
}

@Composable
private fun BlockCell(
    size: Dp,
    label: String,
    ink: Color,
    focused: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    art: @Composable () -> Unit,
) {
    val palette = LocalMimirPalette.current
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .width(size)
            .clip(shape)
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClickLabel = contentDescription,
                onClick = onClick,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(shape)
                .background(if (focused) ink.copy(alpha = 0.16f) else palette.panel)
                .border(
                    width = if (focused) 3.dp else 1.dp,
                    color = when {
                        focused -> ink
                        selected -> ink.copy(alpha = 0.6f)
                        else -> palette.edge.copy(alpha = 0.22f)
                    },
                    shape = shape,
                )
                .padding(6.dp),
            contentAlignment = Alignment.Center,
        ) { art() }
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = if (focused) palette.text else palette.textMuted,
            textAlign = TextAlign.Center,
            maxLines = 2,
            minLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 2.dp),
        )
    }
}

/** An unused slot cell, drawn as the memory card shows it: a dashed empty frame. */
@Composable
private fun EmptyCell(size: Dp) {
    val palette = LocalMimirPalette.current
    Box(
        Modifier
            .width(size)
            .aspectRatio(1f)
            .drawBehind {
                drawRoundRect(
                    palette.gridLine,
                    cornerRadius = CornerRadius(16.dp.toPx()),
                    style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))),
                )
            },
    )
}

@Composable
private fun ToolInfoPanel(tool: ToolEntry, modifier: Modifier, fillHeight: Boolean) {
    val palette = LocalMimirPalette.current
    val ink = tool.ink.resolve(palette)
    ConsolePanel(modifier, accent = ink, contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SaveBlockArt(tool.art, ink, hole = palette.panel, modifier = Modifier.size(52.dp))
            Text(
                stringResource(tool.toolName),
                style = MaterialTheme.typography.headlineLarge,
                color = palette.text,
                modifier = Modifier.weight(1f),
            )
        }
        Text(stringResource(tool.description), style = MaterialTheme.typography.bodyMedium, color = palette.textMuted)
        PanelRule()
        FormatRow(stringResource(R.string.info_reads), tool.reads, palette.text)
        FormatRow(stringResource(R.string.info_writes), tool.writes, ink)
        if (fillHeight) Spacer(Modifier.weight(1f))
        // The enter prompt: what pressing A on this block will do.
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ButtonGlyph("A", color = ink, size = 26.dp)
            Text(stringResource(tool.goal), style = MaterialTheme.typography.titleMedium, color = ink)
        }
    }
}

@Composable
private fun FormatRow(label: String, value: String, valueColor: Color) {
    val palette = LocalMimirPalette.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = palette.textMuted, modifier = Modifier.width(64.dp))
        Text(value, style = MaterialTheme.typography.titleMedium, color = valueColor)
    }
}

/** What the info panel shows before any block has focus, as on a touch launch. */
@Composable
private fun WelcomePanel(modifier: Modifier) {
    val palette = LocalMimirPalette.current
    ConsolePanel(modifier, contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        MimirMark(Modifier.size(56.dp))
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.displaySmall, color = palette.text)
        Text(stringResource(R.string.home_prompt), style = MaterialTheme.typography.bodyLarge, color = palette.textMuted)
    }
}

internal const val YOUTUBE_CHANNEL_URL = "https://youtube.com/@ItsRetroPup"
internal const val KOFI_URL = "https://ko-fi.com/retropup84752"
internal const val BUY_ME_A_COFFEE_URL = "https://buymeacoffee.com/retropupz"
