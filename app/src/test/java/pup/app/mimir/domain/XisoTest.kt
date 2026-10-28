package pup.app.mimir.domain

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream

class XisoTest {
    private val xgd3Offset = Xiso.REDUMP_PARTITION_OFFSETS.last()
    private val xbeData = ByteArray(3000) { (it % 251).toByte() }
    private val mediaData = "hello xbox".toByteArray()

    @Test
    fun detectsRedumpAndXisoLayouts() {
        val redump = redumpImage()
        assertEquals(xgd3Offset, Xiso.findPartitionOffset(redump.reader(), redump.size.toLong()))

        val xiso = rewrite(redump)
        assertEquals(0L, Xiso.findPartitionOffset(xiso.reader(), xiso.size.toLong()))
    }

    @Test
    fun rejectsImagesWithoutXdvdfsHeader() {
        val plainIso = ByteArray(1024 * 1024)
        assertNull(Xiso.findPartitionOffset(plainIso.reader(), plainIso.size.toLong()))
    }

    @Test
    fun trimsVideoPartitionAndPaddingWhileKeepingFileContents() {
        val redump = redumpImage()
        val xiso = rewrite(redump)

        assertTrue(xiso.size < redump.size - xgd3Offset)
        assertEquals(0, xiso.size % 0x10000)

        val root = rootTable(xiso)
        val xbe = root.entry("default.xbe")
        assertArrayEquals(xbeData, xiso.copyOfRange(xbe.sector * 2048, xbe.sector * 2048 + xbe.size))

        val mediaEntry = root.entry("media")
        val media = xiso.copyOfRange(mediaEntry.sector * 2048, mediaEntry.sector * 2048 + mediaEntry.size)
        val file = media.entry("a.bin")
        assertArrayEquals(mediaData, xiso.copyOfRange(file.sector * 2048, file.sector * 2048 + file.size))
        assertEquals(0, media.entry("empty.txt").size)
    }

    @Test
    fun rewritingAnXisoIsStable() {
        val once = rewrite(redumpImage())
        assertArrayEquals(once, rewrite(once))
    }

    private fun rewrite(image: ByteArray): ByteArray =
        ByteArrayOutputStream().also { Xiso.rewrite(image.reader(), image.size.toLong(), it) }.toByteArray()

    /** A Redump-style XGD3 image with a sparse game partition and a nested directory. */
    private fun redumpImage(): ByteArray {
        val partitionSectors = 2100
        val image = ByteArray((xgd3Offset + partitionSectors * 2048L).toInt())
        fun putSector(sector: Int, bytes: ByteArray) =
            bytes.copyInto(image, (xgd3Offset + sector * 2048L).toInt())

        val header = ByteArray(2048)
        MAGIC.copyInto(header, 0)
        header.putU32(20, 300)
        header.putU32(24, 2048)
        MAGIC.copyInto(header, 0x7EC)
        putSector(32, header)

        putSector(300, directoryTable(
            entry(right = 1, sector = 1000, size = xbeData.size, name = "default.xbe"),
            entry(sector = 400, size = 2048, name = "media", directory = true),
        ))
        putSector(400, directoryTable(
            entry(right = 1, sector = 2000, size = mediaData.size, name = "a.bin"),
            entry(sector = 0, size = 0, name = "empty.txt"),
        ))
        putSector(1000, xbeData)
        putSector(2000, mediaData)
        return image
    }

    /** Lays out entries in order; [entry] right pointers are given as entry indexes. */
    private fun directoryTable(vararg entries: ByteArray): ByteArray {
        val table = ByteArray(2048) { 0xFF.toByte() }
        val offsets = entries.runningFold(0) { offset, entry -> offset + (entry.size + 3) / 4 * 4 }
        entries.forEachIndexed { index, entry ->
            val rightIndex = (entry[2].toInt() and 0xFF)
            if (rightIndex != 0) {
                entry[2] = (offsets[rightIndex] / 4).toByte()
                entry[3] = 0
            }
            entry.copyInto(table, offsets[index])
        }
        return table
    }

    private fun entry(right: Int = 0, sector: Int, size: Int, name: String, directory: Boolean = false): ByteArray {
        val bytes = ByteArray(14 + name.length)
        bytes[2] = right.toByte()
        bytes.putU32(4, sector.toLong())
        bytes.putU32(8, size.toLong())
        bytes[12] = if (directory) 0x10 else 0x20
        bytes[13] = name.length.toByte()
        name.toByteArray(Charsets.US_ASCII).copyInto(bytes, 14)
        return bytes
    }

    private data class Entry(val sector: Int, val size: Int)

    private fun rootTable(xiso: ByteArray): ByteArray {
        val header = xiso.copyOfRange(32 * 2048, 33 * 2048)
        val sector = header.u32(20)
        return xiso.copyOfRange(sector * 2048, sector * 2048 + header.u32(24))
    }

    private fun ByteArray.entry(name: String): Entry {
        var offset = 0
        while (offset + 14 <= size && this[offset] != 0xFF.toByte()) {
            val nameLength = this[offset + 13].toInt() and 0xFF
            if (String(this, offset + 14, nameLength, Charsets.US_ASCII) == name) {
                return Entry(u32(offset + 4), u32(offset + 8))
            }
            offset += (14 + nameLength + 3) / 4 * 4
        }
        error("Missing $name")
    }

    private fun ByteArray.reader() = ImageReader { position, buffer, offset, length ->
        require(position + length <= size)
        copyInto(buffer, offset, position.toInt(), position.toInt() + length)
    }

    private fun ByteArray.u32(offset: Int): Int =
        (0 until 4).sumOf { (this[offset + it].toInt() and 0xFF) shl (8 * it) }

    private fun ByteArray.putU32(offset: Int, value: Long) {
        for (index in 0 until 4) this[offset + index] = (value shr (8 * index)).toByte()
    }

    private companion object {
        val MAGIC = "MICROSOFT*XBOX*MEDIA".toByteArray(Charsets.US_ASCII)
    }
}
