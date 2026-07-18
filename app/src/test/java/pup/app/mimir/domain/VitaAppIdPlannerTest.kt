package pup.app.mimir.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VitaAppIdPlannerTest {
    @Test
    fun buildsWritePlansForVitaTitles() {
        val apps = listOf(
            VitaApp(titleId = "PCSA00123", title = "Persona 4 Golden", sourcePath = "app/PCSA00123"),
            VitaApp(titleId = "PCSA00124", title = "Tearaway", sourcePath = "app/PCSA00124"),
        )

        val plan = VitaAppIdPlanner.buildPlan(apps)

        assertEquals(2, plan.changes.size)
        assertEquals(ToolMode.VitaAppIds, plan.mode)
        assertTrue(plan.operations.all { it is FileOperation.WriteTextFile })
        assertEquals("Persona 4 Golden.psvita", plan.changes[0].targetFiles.single())
        assertEquals("PCSA00123", (plan.operations[0] as FileOperation.WriteTextFile).contents)
    }

    @Test
    fun fallsBackToAppIdSuffixWhenTitleConflictsWithinPlan() {
        val apps = listOf(
            VitaApp(titleId = "PCSA00123", title = "Chaos/Child", sourcePath = "app/PCSA00123"),
            VitaApp(titleId = "PCSB00999", title = "Chaos:Child", sourcePath = "app/PCSB00999"),
        )

        val plan = VitaAppIdPlanner.buildPlan(apps)

        assertEquals(2, plan.changes.size)
        assertEquals("Chaos Child.psvita", plan.changes[0].targetFiles.single())
        assertEquals("Chaos Child [PCSB00999].psvita", plan.changes[1].targetFiles.single())
    }

    @Test
    fun buildsDptFilesWithVitaGameIdSection() {
        val app = VitaApp(titleId = "PCSA00123", title = "Persona 4 Golden", sourcePath = "shortcut-db")

        val plan = VitaAppIdPlanner.buildPlan(listOf(app), format = VitaShortcutFormat.Dpt)

        assertEquals("Persona 4 Golden.dpt", plan.changes.single().targetFiles.single())
        assertEquals(
            "[vita_game_id]PCSA00123",
            (plan.operations.single() as FileOperation.WriteTextFile).contents,
        )
    }

    @Test
    fun skipsWhenExistingTargetsBlockPreferredAndFallbackNames() {
        val apps = listOf(
            VitaApp(titleId = "PCSA00123", title = "Persona/4 Golden", sourcePath = "app/PCSA00123"),
        )
        val existingEntries = listOf(
            RomEntry("Persona 4 Golden.psvita", "Persona 4 Golden.psvita"),
            RomEntry("Persona 4 Golden [PCSA00123].psvita", "Persona 4 Golden [PCSA00123].psvita"),
        )

        val plan = VitaAppIdPlanner.buildPlan(apps, existingEntries)

        assertTrue(plan.changes.isEmpty())
        assertEquals("Skipped PCSA00123: target already exists for Persona/4 Golden", plan.conflicts.single())
    }
}
