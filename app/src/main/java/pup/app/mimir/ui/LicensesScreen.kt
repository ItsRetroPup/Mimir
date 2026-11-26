package pup.app.mimir.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.animateScrollBy
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pup.app.mimir.R

/** What a third-party component is used for, shown under its name. */
internal sealed interface LicenseUse {
    /** Mimir's own code. */
    data object ThisApp : LicenseUse

    /** Used everywhere, such as the UI framework and the typeface. */
    data object WholeApp : LicenseUse

    /** Used by one tool; its name comes from that tool's save block. */
    data class Tool(val section: AppSection) : LicenseUse
}

/**
 * One component Mimir ships or downloads, and the notice its license asks us to show. Names,
 * license identifiers and copyright lines are legal text, so they stay untranslated.
 */
internal data class LicenseEntry(
    val name: String,
    val version: String? = null,
    val use: LicenseUse,
    /** SPDX identifier, such as "MIT" or "GPL-2.0-or-later". Null when only upstream terms apply. */
    val license: String?,
    val copyright: String? = null,
    val sourceUrl: String,
    /** The exact source commit, for GPL binaries whose corresponding source must be findable. */
    val sourceRevision: String? = null,
    /** Full license text, a file in assets/licenses. */
    val licenseAsset: String? = null,
)

/**
 * Everything listed in THIRD_PARTY_NOTICES.md, plus the libraries and Python packages that are
 * built into the APK. Keep the two in step when a tool or dependency is added.
 */
internal val licenseEntries = listOf(
    LicenseEntry(
        name = "Mimir",
        use = LicenseUse.ThisApp,
        license = "MIT",
        copyright = "Copyright (c) 2026 RetroPup",
        sourceUrl = "https://github.com/ItsRetroPup/Mimir",
        licenseAsset = "Mimir-MIT.txt",
    ),
    LicenseEntry(
        name = "CHDMan (MAME)",
        use = LicenseUse.Tool(AppSection.ChdMan),
        license = "GPL-2.0-or-later",
        sourceUrl = "https://github.com/mamedev/mame",
        sourceRevision = "ecf0add29f06ba131994dca5b88c3a0edf6c2ad8",
        licenseAsset = "MAME-GPL-2.0.txt",
    ),
    LicenseEntry(
        name = "DolphinTool (Dolphin Emulator)",
        use = LicenseUse.Tool(AppSection.Rvz),
        license = "GPL-2.0-or-later",
        sourceUrl = "https://github.com/dolphin-emu/dolphin",
        sourceRevision = "73de7b8d3e59d4bef685f582d305b65bdb85a707",
        licenseAsset = "Dolphin-GPL-2.0.txt",
    ),
    LicenseEntry(
        name = "Azahar compression",
        version = "2126.0-alpha1",
        use = LicenseUse.Tool(AppSection.Zcci),
        license = "GPL-2.0-or-later",
        sourceUrl = "https://github.com/azahar-emu/azahar",
        sourceRevision = "e11f3da49346a45c400001ea424de298649618c8",
        licenseAsset = "Azahar-GPL-2.0.txt",
    ),
    LicenseEntry(
        name = "NSZ",
        version = "5.0.0",
        use = LicenseUse.Tool(AppSection.Nsz),
        license = "MIT",
        copyright = "Copyright (c) 2019 Nico Bosshard and Blake Warner",
        sourceUrl = "https://github.com/nicoboss/nsz",
        licenseAsset = "NSZ-MIT.txt",
    ),
    LicenseEntry(
        name = "Chaquopy",
        version = "17.0.0",
        use = LicenseUse.Tool(AppSection.Nsz),
        license = "MIT",
        copyright = "Copyright (c) 2017-2025 Chaquo Ltd and contributors",
        sourceUrl = "https://github.com/chaquo/chaquopy",
        licenseAsset = "Chaquopy-MIT.txt",
    ),
    LicenseEntry(
        name = "Python",
        version = "3.13",
        use = LicenseUse.Tool(AppSection.Nsz),
        license = "PSF-2.0",
        copyright = "Copyright (c) 2001-2024 Python Software Foundation; All Rights Reserved",
        sourceUrl = "https://github.com/python/cpython",
        licenseAsset = "Python-PSF-2.0.txt",
    ),
    LicenseEntry(
        name = "python-zstandard",
        version = "0.23.0",
        use = LicenseUse.Tool(AppSection.Nsz),
        license = "BSD-3-Clause",
        copyright = "Copyright (c) 2016, Gregory Szorc",
        sourceUrl = "https://github.com/indygreg/python-zstandard",
        licenseAsset = "zstandard-BSD-3-Clause.txt",
    ),
    LicenseEntry(
        name = "PyCryptodome",
        version = "3.21.0",
        use = LicenseUse.Tool(AppSection.Nsz),
        license = "BSD-2-Clause",
        sourceUrl = "https://github.com/Legrandin/pycryptodome",
        licenseAsset = "PyCryptodome-BSD-2-Clause.txt",
    ),
    // NSZ's own dependencies, installed by pip at build time.
    LicenseEntry(
        name = "Enlighten",
        use = LicenseUse.Tool(AppSection.Nsz),
        license = "MPL-2.0",
        copyright = "Copyright 2017 - 2025 Avram Lubkin",
        sourceUrl = "https://github.com/Rockhopper-Technologies/enlighten",
        licenseAsset = "Enlighten-MPL-2.0.txt",
    ),
    LicenseEntry(
        name = "Prefixed",
        use = LicenseUse.Tool(AppSection.Nsz),
        license = "MPL-2.0",
        copyright = "Copyright 2020 - 2024 Avram Lubkin",
        sourceUrl = "https://github.com/Rockhopper-Technologies/prefixed",
        licenseAsset = "Prefixed-MPL-2.0.txt",
    ),
    LicenseEntry(
        name = "Jinxed",
        use = LicenseUse.Tool(AppSection.Nsz),
        license = "MPL-2.0",
        copyright = "Copyright 2019 - 2026 Avram Lubkin",
        sourceUrl = "https://github.com/Rockhopper-Technologies/jinxed",
        licenseAsset = "Jinxed-MPL-2.0.txt",
    ),
    LicenseEntry(
        name = "Blessed",
        use = LicenseUse.Tool(AppSection.Nsz),
        license = "MIT",
        copyright = "Copyright (c) 2014 Jeff Quast, Copyright (c) 2011 Erik Rose",
        sourceUrl = "https://github.com/jquast/blessed",
        licenseAsset = "Blessed-MIT.txt",
    ),
    LicenseEntry(
        name = "wcwidth",
        use = LicenseUse.Tool(AppSection.Nsz),
        license = "MIT",
        copyright = "Copyright (c) 2014 Jeff Quast",
        sourceUrl = "https://github.com/jquast/wcwidth",
        licenseAsset = "wcwidth-MIT.txt",
    ),
    LicenseEntry(
        name = "ES-DE Android custom systems",
        use = LicenseUse.Tool(AppSection.EsDeSystems),
        license = null,
        copyright = "GlazedBelmont",
        sourceUrl = "https://github.com/GlazedBelmont/es-de-android-custom-systems",
    ),
    LicenseEntry(
        name = "Rubik",
        use = LicenseUse.WholeApp,
        license = "OFL-1.1",
        copyright = "Copyright 2015 The Rubik Project Authors",
        sourceUrl = "https://github.com/googlefonts/rubik",
        licenseAsset = "Rubik-OFL-1.1.txt",
    ),
    LicenseEntry(
        name = "Android Jetpack, Compose and Material Components",
        use = LicenseUse.WholeApp,
        license = "Apache-2.0",
        copyright = "Copyright The Android Open Source Project",
        sourceUrl = "https://github.com/androidx/androidx",
        licenseAsset = "Apache-2.0.txt",
    ),
    LicenseEntry(
        name = "Kotlin and kotlinx.coroutines",
        use = LicenseUse.WholeApp,
        license = "Apache-2.0",
        copyright = "Copyright JetBrains s.r.o. and Kotlin Programming Language contributors",
        sourceUrl = "https://github.com/JetBrains/kotlin",
        licenseAsset = "Apache-2.0.txt",
    ),
)

/**
 * Licenses, opened from the footer on home: every tool and library Mimir is built with, and its
 * license. Selecting a row opens the full license text. Laid out like Stats: an intro panel beside
 * the list on a wide screen, stacked on a narrow one.
 */
@Composable
internal fun LicensesScreen(contentFocus: FocusRequester, onOpenUrl: (String) -> Unit, modifier: Modifier = Modifier) {
    // Saved by index so the open license survives rotation; -1 means none is open.
    var openIndex by rememberSaveable { mutableStateOf(-1) }
    val list: @Composable () -> Unit = {
        licenseEntries.forEachIndexed { index, entry ->
            LicenseRow(
                entry = entry,
                onClick = { openIndex = index },
                modifier = if (index == 0) Modifier.focusRequester(contentFocus) else Modifier,
            )
        }
    }
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
                ) { LicensesIntro() }
                Column(
                    Modifier
                        .weight(0.58f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) { list() }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                LicensesIntro()
                list()
            }
        }
    }
    licenseEntries.getOrNull(openIndex)?.let { entry ->
        LicenseDialog(entry, onOpenUrl = onOpenUrl, onDismiss = { openIndex = -1 })
    }
}

@Composable
private fun LicensesIntro() {
    val palette = LocalMimirPalette.current
    ConsolePanel(Modifier.fillMaxWidth(), contentPadding = PaddingValues(20.dp)) {
        Text(stringResource(R.string.licenses), style = MaterialTheme.typography.headlineSmall, color = palette.text)
        Text(stringResource(R.string.licenses_intro), style = MaterialTheme.typography.bodyMedium, color = palette.textMuted)
    }
}

/** The line under a component's name: what it's for. */
@Composable
private fun useLabel(use: LicenseUse): String = when (use) {
    LicenseUse.ThisApp -> stringResource(R.string.licenses_this_app)
    LicenseUse.WholeApp -> stringResource(R.string.licenses_used_throughout)
    is LicenseUse.Tool -> {
        val tool = toolFor(use.section)
        if (tool != null) stringResource(R.string.licenses_used_by, stringResource(tool.toolName))
        else stringResource(R.string.licenses_used_throughout)
    }
}

/** One component as a row: name and use on the left, its license as a tag on the right. */
@Composable
private fun LicenseRow(entry: LicenseEntry, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val palette = LocalMimirPalette.current
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(shape)
            .background(palette.panel)
            .focusRing(shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                listOfNotNull(entry.name, entry.version).joinToString(" "),
                style = MaterialTheme.typography.titleMedium,
                color = palette.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(useLabel(entry.use), style = MaterialTheme.typography.bodySmall, color = palette.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        LicenseTag(entry.license ?: stringResource(R.string.licenses_upstream_terms))
    }
}

/** A license identifier in a small outlined pill, in the page's ink. */
@Composable
private fun LicenseTag(label: String) {
    val ink = MaterialTheme.colorScheme.primary
    Text(
        label,
        style = MaterialTheme.typography.labelMedium,
        color = ink,
        maxLines = 1,
        modifier = Modifier
            .border(1.dp, ink, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

/**
 * A component's notice and full license text. The text scrolls with the D-pad: up and down scroll
 * it while there is more to see, then let focus move on to the buttons.
 */
@Composable
private fun LicenseDialog(entry: LicenseEntry, onOpenUrl: (String) -> Unit, onDismiss: () -> Unit) {
    val palette = LocalMimirPalette.current
    val context = LocalContext.current
    // Read the license file off the main thread; it shows once loaded.
    val licenseText by produceState<String?>(initialValue = null, entry.licenseAsset) {
        value = entry.licenseAsset?.let { asset ->
            withContext(Dispatchers.IO) {
                runCatching { context.assets.open("licenses/$asset").bufferedReader().use { it.readText() } }.getOrNull()
            }
        }
    }
    val scroll = rememberScrollState()
    val scope = rememberCoroutineScope()
    val step = with(LocalDensity.current) { 96.dp.toPx() }
    val textFocus = remember { FocusRequester() }
    val inputModeManager = LocalInputModeManager.current
    LaunchedEffect(Unit) {
        // With a controller, start on the text so the D-pad scrolls it straight away.
        if (inputModeManager.inputMode == InputMode.Keyboard) {
            withFrameNanos { }
            textFocus.requestFocus()
        }
    }
    val dpadScroll = Modifier.onPreviewKeyEvent { event ->
        val down = when (event.key) {
            Key.DirectionDown -> true
            Key.DirectionUp -> false
            else -> return@onPreviewKeyEvent false
        }
        val canScroll = if (down) scroll.value < scroll.maxValue else scroll.value > 0
        if (!canScroll) return@onPreviewKeyEvent false
        if (event.type == KeyEventType.KeyDown) scope.launch { scroll.animateScrollBy(if (down) step else -step) }
        true
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(listOfNotNull(entry.name, entry.version).joinToString(" ")) },
        text = {
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .focusRing(RoundedCornerShape(8.dp))
                    .then(dpadScroll)
                    .focusRequester(textFocus)
                    .focusable()
                    .verticalScroll(scroll),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(useLabel(entry.use), style = MaterialTheme.typography.bodyMedium, color = palette.textMuted)
                LicenseTag(entry.license ?: stringResource(R.string.licenses_upstream_terms))
                entry.copyright?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = palette.text) }
                Text(entry.sourceUrl, style = MaterialTheme.typography.bodySmall, color = palette.textMuted)
                entry.sourceRevision?.let {
                    Text(stringResource(R.string.licenses_revision, it), style = MaterialTheme.typography.bodySmall, color = palette.textMuted)
                }
                if (entry.licenseAsset == null) {
                    Text(stringResource(R.string.licenses_upstream_body), style = MaterialTheme.typography.bodySmall, color = palette.textMuted)
                }
                licenseText?.let {
                    PanelRule()
                    Text(it, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace), color = palette.text)
                }
            }
        },
        confirmButton = {
            OutlinedButton(onClick = onDismiss, modifier = Modifier.focusRing()) { Text(stringResource(R.string.action_close)) }
        },
        dismissButton = {
            OutlinedButton(onClick = { onOpenUrl(entry.sourceUrl) }, modifier = Modifier.focusRing()) {
                Text(stringResource(R.string.action_view_source))
            }
        },
    )
}
