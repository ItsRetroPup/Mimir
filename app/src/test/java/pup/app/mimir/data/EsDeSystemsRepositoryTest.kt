package pup.app.mimir.data

import org.junit.Assert.assertEquals
import org.junit.Test

class EsDeSystemsRepositoryTest {
    @Test
    fun convertsPrimarySafTreeToAbsoluteAndroidPath() {
        assertEquals(
            "/storage/emulated/0/ROMs/Switch",
            EsDeSystemsRepository.absolutePathForDocumentId("primary:ROMs/Switch"),
        )
    }

    @Test
    fun convertsRemovableSafTreeToVolumePath() {
        assertEquals(
            "/storage/ABCD-1234/Roms/switch",
            EsDeSystemsRepository.absolutePathForDocumentId("ABCD-1234:Roms/switch"),
        )
    }
}
