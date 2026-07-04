package com.mimir.companion.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RomScannerTest {
    @Test
    fun detectsCommonDiscPatterns() {
        val entries = listOf(
            RomEntry("psx/Final Fantasy VII (Disc 1).chd", "Final Fantasy VII (Disc 1).chd"),
            RomEntry("psx/Final Fantasy VII (Disc 2).chd", "Final Fantasy VII (Disc 2).chd"),
            RomEntry("psx/Final Fantasy VII (Disc 3).chd", "Final Fantasy VII (Disc 3).chd"),
        )

        val result = RomScanner.scan(entries)

        assertEquals(1, result.discSets.size)
        assertEquals("Final Fantasy VII", result.discSets.first().title)
    }

    @Test
    fun ignoresSingleDiscGames() {
        val entries = listOf(
            RomEntry("psx/Metal Gear Solid (Disc 1).chd", "Metal Gear Solid (Disc 1).chd"),
            RomEntry("psx/Crash Bandicoot.chd", "Crash Bandicoot.chd"),
        )

        val result = RomScanner.scan(entries)

        assertTrue(result.discSets.isEmpty())
    }

    @Test
    fun buildsEsDeFolderAsFilePlan() {
        val entries = listOf(
            RomEntry("psx/Xenogears (Disc 1).chd", "Xenogears (Disc 1).chd"),
            RomEntry("psx/Xenogears (Disc 2).chd", "Xenogears (Disc 2).chd"),
        )

        val plan = ChangePlanner.buildPlan(RomScanner.scan(entries), FrontendPreset.EsDe)

        assertTrue(plan.conflicts.isEmpty())
        assertEquals("psx/Xenogears.m3u/Xenogears.m3u", plan.changes.first().detailPath)
        assertEquals(4, plan.operations.size)
    }

    @Test
    fun buildsTemporaryDefaultPlaylistPlanWithoutMovingDiscs() {
        val entries = listOf(
            RomEntry("saturn/Panzer Dragoon Saga (Disc 1).cue", "Panzer Dragoon Saga (Disc 1).cue"),
            RomEntry("saturn/Panzer Dragoon Saga (Disc 2).cue", "Panzer Dragoon Saga (Disc 2).cue"),
        )

        val plan = ChangePlanner.buildPlan(RomScanner.scan(entries), FrontendPreset.Daijisho)

        assertTrue(plan.conflicts.isEmpty())
        assertEquals("saturn/Panzer Dragoon Saga.m3u", plan.changes.first().detailPath)
        assertEquals(1, plan.operations.size)
        assertTrue(plan.operations.single() is FileOperation.WriteTextFile)
        assertTrue(
            plan.changes.first().targetFiles == entries.map { it.relativePath }
        )
    }

    @Test
    fun buildsCocoonPlaylistWithoutMovingDiscs() {
        val entries = listOf(
            RomEntry("dreamcast/Shenmue (Disc 1).chd", "Shenmue (Disc 1).chd"),
            RomEntry("dreamcast/Shenmue (Disc 2).chd", "Shenmue (Disc 2).chd"),
        )

        val plan = ChangePlanner.buildPlan(RomScanner.scan(entries), FrontendPreset.Cocoon)

        assertTrue(plan.conflicts.isEmpty())
        assertEquals("dreamcast/Shenmue.m3u", plan.changes.first().detailPath)
        assertEquals(1, plan.operations.size)
        assertTrue(plan.operations.single() is FileOperation.WriteTextFile)
        assertEquals(
            entries.map { it.relativePath },
            plan.changes.first().targetFiles,
        )
    }

    @Test
    fun skipsOnlyConflictingDiscSets() {
        val entries = listOf(
            RomEntry("psx/Xenogears (Disc 1).chd", "Xenogears (Disc 1).chd"),
            RomEntry("psx/Xenogears (Disc 2).chd", "Xenogears (Disc 2).chd"),
            RomEntry("psx/Xenogears.m3u", "Xenogears.m3u"),
            RomEntry("psx/Grandia (Disc 1).chd", "Grandia (Disc 1).chd"),
            RomEntry("psx/Grandia (Disc 2).chd", "Grandia (Disc 2).chd"),
        )

        val plan = ChangePlanner.buildPlan(RomScanner.scan(entries), FrontendPreset.Cocoon)

        assertEquals(1, plan.conflicts.size)
        assertEquals("Skipped Xenogears: target already exists: psx/Xenogears.m3u", plan.conflicts.single())
        assertEquals(1, plan.changes.size)
        assertEquals("Grandia", plan.changes.single().title)
    }
}
