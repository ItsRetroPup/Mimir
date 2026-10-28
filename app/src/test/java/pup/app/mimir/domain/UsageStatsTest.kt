package pup.app.mimir.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class UsageStatsTest {
    @Test
    fun recordAddsToEachToolAndTheTotals() {
        val stats = UsageStats(sinceMillis = 1L)
            .record("Chd", games = 3, spaceSavedBytes = 3_000L)
            .record("Chd", games = 2, spaceSavedBytes = 1_000L)
            .record("RomZipper", games = 4, spaceSavedBytes = 500L)

        assertEquals(ToolUsage(games = 5, spaceSavedBytes = 4_000L), stats.usage("Chd"))
        assertEquals(9, stats.totalGames)
        assertEquals(4_500L, stats.totalSpaceSavedBytes)
        assertEquals(ToolUsage(), stats.usage("XboxXiso"))
    }

    @Test
    fun aRunThatDidNothingLeavesTheStatsUnchanged() {
        val stats = UsageStats(sinceMillis = 1L)

        assertSame(stats, stats.record("Chd", games = 0, spaceSavedBytes = 0L))
    }

    @Test
    fun totalSpaceSavedNeverShowsBelowZero() {
        val stats = UsageStats(sinceMillis = 1L).record("Chd", games = 1, spaceSavedBytes = -200L)

        assertEquals(-200L, stats.usage("Chd").spaceSavedBytes)
        assertEquals(0L, stats.totalSpaceSavedBytes)
    }

    @Test
    fun keysUseTheConverterForConverterToolsAndTheModeOtherwise() {
        assertEquals("Chd", UsageStats.keyFor(ToolMode.Converter, ConverterTool.Chd))
        assertEquals("RomPatcher", UsageStats.keyFor(ToolMode.RomPatcher))
    }

    @Test
    fun onlyCompressingToolsCountTowardSpaceSaved() {
        assertTrue(UsageStats.savesSpace(ToolMode.RomZipper, null))
        assertTrue(UsageStats.savesSpace(ToolMode.Converter, ConverterTool.Chd))
        assertTrue(UsageStats.savesSpace(ToolMode.Converter, ConverterTool.XboxXiso))
        assertFalse(UsageStats.savesSpace(ToolMode.Converter, ConverterTool.NszNsp))
        assertFalse(UsageStats.savesSpace(ToolMode.MultiDiscOrganizer, null))
    }

    @Test
    fun completedChangesCountsOnlyGamesWhoseOperationsAllRan() {
        val twoStepGame = PlannedChange(
            title = "Game A",
            sourceFiles = emptyList(),
            targetFiles = emptyList(),
            detailLabel = "",
            detailPath = "a",
            operations = listOf(FileOperation.CreateDirectory("a"), FileOperation.MoveFile("a.cue", "a/a.cue")),
        )
        val oneStepGame = twoStepGame.copy(title = "Game B", detailPath = "b", operations = listOf(FileOperation.CreateDirectory("b")))
        val plan = OperationPlan(
            mode = ToolMode.MultiDiscOrganizer,
            changes = listOf(twoStepGame, oneStepGame),
            operations = twoStepGame.operations + oneStepGame.operations,
            conflicts = emptyList(),
        )

        assertEquals(0, plan.completedChanges(0))
        assertEquals(0, plan.completedChanges(1))
        assertEquals(1, plan.completedChanges(2))
        assertEquals(2, plan.completedChanges(3))
    }
}
