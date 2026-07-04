package com.mimir.companion.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RomZipperPlannerTest {
    @Test
    fun buildsZipPlanForWhitelistedExtensions() {
        val entries = listOf(
            RomEntry("nds/Mario Kart DS.nds", "Mario Kart DS.nds"),
            RomEntry("gba/Metroid Fusion.gba", "Metroid Fusion.gba"),
            RomEntry("psx/Crash Bandicoot.bin", "Crash Bandicoot.bin"),
        )

        val plan = RomZipperPlanner.buildPlan(entries)

        assertEquals(ToolMode.RomZipper, plan.mode)
        assertTrue(plan.conflicts.isEmpty())
        assertEquals(2, plan.changes.size)
        assertEquals(
            listOf("gba/Metroid Fusion.zip", "nds/Mario Kart DS.zip"),
            plan.changes.map { it.detailPath }.sorted(),
        )
        assertTrue(plan.operations.all { it is FileOperation.ZipFile })
    }

    @Test
    fun reportsConflictWhenZipAlreadyExists() {
        val entries = listOf(
            RomEntry("nes/Zelda.nes", "Zelda.nes"),
            RomEntry("nes/Zelda.zip", "Zelda.zip"),
        )

        val plan = RomZipperPlanner.buildPlan(entries)

        assertEquals(1, plan.conflicts.size)
        assertEquals("Skipped Zelda.nes: target already exists: nes/Zelda.zip", plan.conflicts.single())
        assertTrue(plan.changes.isEmpty())
        assertTrue(plan.operations.isEmpty())
    }

    @Test
    fun skipsOnlyConflictingZipTargets() {
        val entries = listOf(
            RomEntry("gba/Metroid Fusion.gba", "Metroid Fusion.gba"),
            RomEntry("gba/Metroid Fusion.zip", "Metroid Fusion.zip"),
            RomEntry("nds/Mario Kart DS.nds", "Mario Kart DS.nds"),
        )

        val plan = RomZipperPlanner.buildPlan(entries)

        assertEquals(1, plan.conflicts.size)
        assertEquals(1, plan.changes.size)
        assertEquals("nds/Mario Kart DS.zip", plan.changes.single().detailPath)
    }
}
