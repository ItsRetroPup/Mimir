package pup.app.mimir.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeConverterPlannerTest {
    @Test
    fun buildsRvzTargetsAndMarksExistingOutputForOverwrite() {
        val plan = NativeConverterPlanner.buildPlan(
            entries = listOf(
                RomEntry("wii/Metroid.iso", "Metroid.iso", sizeBytes = 42L),
                RomEntry("wii/Metroid.rvz", "Metroid.rvz"),
                RomEntry("wii/Ignore.wbfs", "Ignore.wbfs"),
            ),
            tool = ConverterTool.DolphinRvz,
        )

        assertEquals(1, plan.changes.size)
        assertEquals("wii/Metroid.rvz", plan.changes.single().detailPath)
        assertTrue(plan.changes.single().targetAlreadyExists)
        val operation = plan.operations.single() as FileOperation.ConvertWithTool
        assertEquals(ConverterTool.DolphinRvz, operation.tool)
    }

    @Test
    fun onlyAcceptsRequestedToolInputExtensions() {
        val plan = NativeConverterPlanner.buildPlan(
            entries = listOf(
                RomEntry("3ds/Game.3ds", "Game.3ds"),
                RomEntry("3ds/Homebrew.3dsx", "Homebrew.3dsx"),
                RomEntry("3ds/Cart.cci", "Cart.cci"),
            ),
            tool = ConverterTool.AzaharZcci,
        )

        assertEquals(listOf("3ds/Cart.zcci", "3ds/Game.zcci"), plan.changes.map { it.detailPath })
        assertFalse(plan.changes.any { it.title == "Homebrew.3dsx" })
    }

    @Test
    fun buildsNspTargetsFromNszPackagesOnly() {
        val plan = NativeConverterPlanner.buildPlan(
            entries = listOf(
                RomEntry("switch/Game.nsz", "Game.nsz", sizeBytes = 1024L),
                RomEntry("switch/Update.NSZ", "Update.NSZ", sizeBytes = 512L),
                RomEntry("switch/Existing.nsp", "Existing.nsp"),
            ),
            tool = ConverterTool.NszNsp,
        )

        assertEquals(listOf("switch/Game.nsp", "switch/Update.nsp"), plan.changes.map { it.detailPath })
        assertEquals(
            listOf(ConverterTool.NszNsp, ConverterTool.NszNsp),
            plan.operations.map { (it as FileOperation.ConvertWithTool).tool },
        )
    }

    @Test
    fun buildsNszTargetsFromNspPackagesAndFlagsExistingNsz() {
        val plan = NativeConverterPlanner.buildPlan(
            entries = listOf(
                RomEntry("switch/Game.nsp", "Game.nsp", sizeBytes = 4096L),
                RomEntry("switch/Game.nsz", "Game.nsz"),
                RomEntry("switch/Dlc.NSP", "Dlc.NSP", sizeBytes = 256L),
                RomEntry("switch/Cart.xci", "Cart.xci"),
            ),
            tool = ConverterTool.NspNsz,
        )

        assertEquals(listOf("switch/Dlc.nsz", "switch/Game.nsz"), plan.changes.map { it.detailPath })
        assertEquals(listOf(false, true), plan.changes.map { it.targetAlreadyExists })
        assertTrue(plan.operations.all { (it as FileOperation.ConvertWithTool).tool == ConverterTool.NspNsz })
    }

    @Test
    fun bothSwitchConvertersNeedProdKeys() {
        assertTrue(ConverterTool.NszNsp.needsProdKeys)
        assertTrue(ConverterTool.NspNsz.needsProdKeys)
        assertFalse(ConverterTool.DolphinRvz.needsProdKeys)
        assertFalse(ConverterTool.NspNsz.replacesSource)
    }

    @Test
    fun xisoReplacesSourceWithoutMarkingItAsAnOverwrite() {
        val plan = NativeConverterPlanner.buildPlan(
            entries = listOf(
                RomEntry("xbox/Halo.iso", "Halo.iso", sizeBytes = 7_825_162_240L),
                RomEntry("xbox/Readme.txt", "Readme.txt"),
            ),
            tool = ConverterTool.XboxXiso,
        )

        val change = plan.changes.single()
        assertEquals("xbox/Halo.iso", change.detailPath)
        assertFalse(change.targetAlreadyExists)
        val operation = plan.operations.single() as FileOperation.ConvertWithTool
        assertEquals("xbox/Halo.iso", operation.sourcePath)
        assertEquals("xbox/Halo.iso", operation.targetPath)
    }
}
