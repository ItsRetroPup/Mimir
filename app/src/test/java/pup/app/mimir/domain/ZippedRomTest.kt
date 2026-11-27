package pup.app.mimir.domain

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class ZippedRomTest {
    private fun zipOf(vararg files: Pair<String, ByteArray>): ByteArray =
        ByteArrayOutputStream().also { buffer ->
            ZipOutputStream(buffer).use { zip ->
                files.forEach { (name, bytes) ->
                    zip.putNextEntry(ZipEntry(name))
                    zip.write(bytes)
                    zip.closeEntry()
                }
            }
        }.toByteArray()

    @Test
    fun picksTheRomOverAReadme() {
        val rom = ByteArray(1024) { it.toByte() }
        val zip = zipOf("readme.txt" to "hi".toByteArray(), "Game.sfc" to rom)

        val entry = ZippedRom.largestEntry { ByteArrayInputStream(zip) }

        assertEquals("Game.sfc", entry.name)
        assertEquals(1024L, entry.size)
        assertArrayEquals(rom, ZippedRom.read(ByteArrayInputStream(zip), entry.name))
    }

    @Test
    fun patchedRomRoundTripsThroughAZip() {
        val rom = ByteArray(64)
        val ips = "PATCH".toByteArray() + byteArrayOf(0, 0, 0x10, 0, 5) + "HELLO".toByteArray() + "EOF".toByteArray()
        val patched = RomPatcher.apply(rom, ips)

        val zipped = ZippedRom.zip("Hack.sfc", patched)
        val entry = ZippedRom.largestEntry { ByteArrayInputStream(zipped) }

        assertEquals("Hack.sfc", entry.name)
        assertArrayEquals(patched, ZippedRom.read(ByteArrayInputStream(zipped), "Hack.sfc"))
        assertEquals("HELLO", String(patched, 0x10, 5))
    }
}
