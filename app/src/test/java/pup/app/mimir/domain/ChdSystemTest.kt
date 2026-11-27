package pup.app.mimir.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChdSystemTest {
    @Test
    fun dreamcastHasExpectedFolderAliasesAndInputs() {
        val system = ChdSystem.Dreamcast

        assertTrue(listOf("dreamcast", "dc", "Sega - Dreamcast").all { SystemFolders.matches(it, system.folderAliases) })
        assertTrue(setOf("gdi", "cue", "iso").all { it in system.supportedExtensions })
    }

    @Test
    fun ps3ReadsIsoImagesFromItsFolders() {
        assertTrue(listOf("ps3", "PlayStation 3").all { SystemFolders.matches(it, ChdSystem.PlayStation3.folderAliases) })
        assertEquals(setOf("iso"), ChdSystem.PlayStation3.supportedExtensions)
    }

    @Test
    fun segaCdIncludesMegaCdAliases() {
        val aliases = ChdSystem.SegaCd.folderAliases
        assertTrue(
            listOf("sega cd", "segacd", "Mega CD", "megacd", "Sega - Mega CD & Sega CD")
                .all { SystemFolders.matches(it, aliases) },
        )
    }
}
