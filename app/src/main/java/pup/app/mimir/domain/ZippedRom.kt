package pup.app.mimir.domain

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** Reads the ROM out of a zip and zips a patched ROM back up, so the ROM Patcher can take zipped ROMs. */
object ZippedRom {
    data class Entry(val name: String, val size: Long)

    /**
     * The ROM in a zip: its largest file, so a readme or cover beside it is skipped. [open] is
     * called once; sizes are counted because zips don't always record them up front.
     */
    fun largestEntry(open: () -> InputStream): Entry {
        var best: Entry? = null
        ZipInputStream(open()).use { zip ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val entry = zip.nextEntry ?: break
                if (entry.isDirectory) continue
                var size = 0L
                while (true) {
                    val read = zip.read(buffer)
                    if (read < 0) break
                    size += read
                }
                if (best == null || size > best!!.size) best = Entry(entry.name, size)
            }
        }
        return best ?: error("This zip file has no ROM inside.")
    }

    /** The bytes of the file called [name] in the zip read from [input]. */
    fun read(input: InputStream, name: String): ByteArray = ZipInputStream(input).use { zip ->
        while (true) {
            val entry = zip.nextEntry ?: break
            if (entry.name == name) return@use zip.readBytes()
        }
        error("Unable to read $name from the zip.")
    }

    /** A zip holding [bytes] as [entryName]. */
    fun zip(entryName: String, bytes: ByteArray): ByteArray =
        ByteArrayOutputStream().also { buffer ->
            ZipOutputStream(buffer).use { zip ->
                zip.putNextEntry(ZipEntry(entryName))
                zip.write(bytes)
                zip.closeEntry()
            }
        }.toByteArray()
}
