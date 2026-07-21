package pup.app.mimir.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChdPlannerTest {
    @Test
    fun plansDreamcastGdiAsCdChd() {
        val plan = ChdPlanner.buildPlan(
            entries = listOf(
                RomEntry("dreamcast/Jet Set Radio.gdi", "Jet Set Radio.gdi"),
                RomEntry("dreamcast/Jet Set Radio track01.bin", "Jet Set Radio track01.bin"),
            ),
            system = ChdSystem.Dreamcast,
            discType = ChdDiscType.Cd,
            deleteOriginalFiles = false,
        )

        assertEquals(ToolMode.ChdConverter, plan.mode)
        assertEquals(listOf("dreamcast/Jet Set Radio.chd"), plan.changes.map { it.detailPath })
        val operation = plan.operations.single() as FileOperation.ConvertToChd
        assertEquals("dreamcast/Jet Set Radio.gdi", operation.sourcePath)
        assertEquals(ChdDiscType.Cd, operation.discType)
        assertTrue(!operation.deleteOriginalFiles)
    }

    @Test
    fun filtersBySelectedSystemAndUsesDvdConversion() {
        val plan = ChdPlanner.buildPlan(
            entries = listOf(
                RomEntry("ps2/Shadow of the Colossus.iso", "Shadow of the Colossus.iso"),
                RomEntry("ps2/Other Game.cue", "Other Game.cue"),
            ),
            system = ChdSystem.PlayStation2,
            discType = ChdDiscType.Dvd,
            deleteOriginalFiles = false,
        )

        assertEquals(1, plan.changes.size)
        assertEquals("ps2/Shadow of the Colossus.chd", plan.changes.single().detailPath)
        assertEquals(ChdDiscType.Dvd, (plan.operations.single() as FileOperation.ConvertToChd).discType)
    }

    @Test
    fun marksExistingChdForExplicitOverwriteSelection() {
        val plan = ChdPlanner.buildPlan(
            entries = listOf(
                RomEntry("psp/Patapon.iso", "Patapon.iso"),
                RomEntry("psp/Patapon.chd", "Patapon.chd"),
            ),
            system = ChdSystem.PlayStationPortable,
            discType = ChdDiscType.Dvd,
            deleteOriginalFiles = true,
        )

        assertEquals(1, plan.changes.size)
        assertTrue(plan.changes.single().targetAlreadyExists)
        assertTrue(plan.conflicts.isEmpty())
    }

    @Test
    fun matchesExistingChdWithoutConsideringExtensionCase() {
        val plan = ChdPlanner.buildPlan(
            entries = listOf(
                RomEntry("psp/Patapon.iso", "Patapon.iso"),
                RomEntry("psp/Patapon.CHD", "Patapon.CHD"),
            ),
            system = ChdSystem.PlayStationPortable,
            discType = ChdDiscType.Dvd,
            deleteOriginalFiles = false,
        )

        assertEquals("psp/Patapon.CHD", plan.changes.single().detailPath)
        assertTrue(plan.changes.single().targetAlreadyExists)
    }

    @Test
    fun carriesTheDeleteOriginalFilesChoiceIntoThePlan() {
        val plan = ChdPlanner.buildPlan(
            entries = listOf(RomEntry("psp/Patapon.iso", "Patapon.iso")),
            system = ChdSystem.PlayStationPortable,
            discType = ChdDiscType.Dvd,
            deleteOriginalFiles = true,
        )

        assertTrue((plan.operations.single() as FileOperation.ConvertToChd).deleteOriginalFiles)
    }

    @Test
    fun cueCompanionBinsAreNotSeparateConversionTargets() {
        val plan = ChdPlanner.buildPlan(
            entries = listOf(
                RomEntry("dc/Sonic Adventure.cue", "Sonic Adventure.cue"),
                RomEntry("dc/Sonic Adventure (Track 1).bin", "Sonic Adventure (Track 1).bin"),
            ),
            system = ChdSystem.Dreamcast,
            discType = ChdDiscType.Cd,
            deleteOriginalFiles = false,
        )

        assertEquals(listOf("dc/Sonic Adventure.chd"), plan.changes.map { it.detailPath })
    }
}
