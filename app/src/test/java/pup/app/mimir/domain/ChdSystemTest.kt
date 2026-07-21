package pup.app.mimir.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChdSystemTest {
    @Test
    fun dreamcastHasExpectedFolderAliasesAndInputs() {
        val system = ChdSystem.Dreamcast

        assertEquals(setOf("dreamcast", "dc"), system.folderAliases)
        assertTrue(setOf("gdi", "cue", "iso").all { it in system.supportedExtensions })
    }

    @Test
    fun segaCdIncludesMegaCdAliases() {
        assertEquals(
            setOf("sega cd", "segacd", "mega cd", "megacd"),
            ChdSystem.SegaCd.folderAliases,
        )
    }
}
