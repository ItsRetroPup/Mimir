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
}
