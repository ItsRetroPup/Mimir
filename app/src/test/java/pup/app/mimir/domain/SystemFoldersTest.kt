package pup.app.mimir.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SystemFoldersTest {
    private val azahar = ConverterTool.AzaharZcci.folderAliases

    @Test
    fun azaharFindsEveryCommon3dsFolderName() {
        listOf(
            "3ds", "3DS", "n3ds", "N3DS", "Nintendo 3DS", "nintendo_3ds", "Nintendo-3DS", "nintendo3ds",
            "New Nintendo 3DS", "Nintendo - Nintendo 3DS", "Nintendo - Nintendo 3DS (Decrypted)", "3DS [EU]",
        ).forEach { name -> assertTrue(name, SystemFolders.matches(name, azahar)) }
    }

    @Test
    fun azaharIgnoresOtherSystems() {
        listOf("ds", "nds", "Nintendo DS", "switch", "3ds saves", "").forEach { name ->
            assertFalse(name, SystemFolders.matches(name, azahar))
        }
    }

    @Test
    fun everyToolMatchesItsOwnAliasesWrittenDifferently() {
        val aliasSets = ConverterTool.entries.map { it.folderAliases } + ChdSystem.entries.map { it.folderAliases }
        aliasSets.forEach { aliases ->
            aliases.forEach { alias ->
                assertTrue(alias, SystemFolders.matches(alias.uppercase(), aliases))
                assertTrue(alias, SystemFolders.matches(alias.replace(' ', '_'), aliases))
            }
        }
    }

    @Test
    fun otherToolsFindTheirCommonNames() {
        assertTrue(SystemFolders.matches("Nintendo GameCube", ConverterTool.DolphinRvz.folderAliases))
        assertTrue(SystemFolders.matches("GCN", ConverterTool.DolphinRvz.folderAliases))
        assertTrue(SystemFolders.matches("Nintendo - Wii", ConverterTool.DolphinRvz.folderAliases))
        assertTrue(SystemFolders.matches("NSW", ConverterTool.NspNsz.folderAliases))
        assertTrue(SystemFolders.matches("Nintendo Switch", ConverterTool.NszNsp.folderAliases))
        assertTrue(SystemFolders.matches("Microsoft - Xbox", ConverterTool.XboxXiso.folderAliases))
        assertTrue(SystemFolders.matches("PSX", ChdSystem.PlayStation1.folderAliases))
        assertTrue(SystemFolders.matches("Sony - PlayStation", ChdSystem.PlayStation1.folderAliases))
        assertTrue(SystemFolders.matches("Sony - PlayStation Portable", ChdSystem.PlayStationPortable.folderAliases))
        assertTrue(SystemFolders.matches("Sega Saturn", ChdSystem.SegaSaturn.folderAliases))
        // A PS2 folder isn't mistaken for PS1, or an Xbox 360 folder for the original Xbox.
        assertFalse(SystemFolders.matches("PS2", ChdSystem.PlayStation1.folderAliases))
        assertFalse(SystemFolders.matches("Xbox 360", ConverterTool.XboxXiso.folderAliases))
    }
}
