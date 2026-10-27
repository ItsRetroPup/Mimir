package pup.app.mimir.domain

import java.io.OutputStream

/** Random-access view of a disc image. */
fun interface ImageReader {
    /** Fills [length] bytes of [buffer] from [position], failing if the image ends first. */
    fun readFully(position: Long, buffer: ByteArray, offset: Int, length: Int)
}

/**
 * Reads the original Xbox XDVDFS filesystem and rewrites it as a trimmed XISO: the Redump video
 * partition and unused padding are dropped, and directories and files are packed contiguously.
 */
object Xiso {
    const val SECTOR_SIZE = 2048L

    /** Game partition offsets used by Redump dumps of XGD1, XGD2, and XGD3 discs. */
    val REDUMP_PARTITION_OFFSETS = listOf(0x18300000L, 0x0FD90000L, 0x02080000L)

    private const val HEADER_SECTOR = 32L
    private const val FIRST_DATA_SECTOR = 0x108L
    private const val IMAGE_MODULUS = 0x10000L
    private const val TRAILING_MAGIC_OFFSET = 0x7EC
    private const val DIRECTORY_ATTRIBUTE = 0x10
    private const val ENTRY_HEADER_SIZE = 14
    private const val NO_CHILD = 0xFFFF
    private const val MAX_DIRECTORY_TABLE_BYTES = 64 * 1024 * 1024
    private const val COPY_BUFFER_BYTES = 1024 * 1024
    private val MAGIC = "MICROSOFT*XBOX*MEDIA".toByteArray(Charsets.US_ASCII)

    /** Returns where the XDVDFS partition starts, or null when the image is not an Xbox disc. */
    fun findPartitionOffset(reader: ImageReader, imageSize: Long): Long? =
        (listOf(0L) + REDUMP_PARTITION_OFFSETS).firstOrNull { offset ->
            offset + (HEADER_SECTOR + 1) * SECTOR_SIZE <= imageSize &&
                hasMagic(readHeader(reader, offset))
        }

    /** Writes a trimmed XISO of [reader] to [output] and returns the number of bytes written. */
    fun rewrite(
        reader: ImageReader,
        imageSize: Long,
        output: OutputStream,
        onProgress: (Float) -> Unit = {},
        ensureActive: () -> Unit = {},
    ): Long {
        val partition = findPartitionOffset(reader, imageSize)
            ?: error("Not an Xbox disc image.")
        val header = readHeader(reader, partition)
        val rootSector = header.u32(20)
        val rootSize = header.u32(24)
        require(rootSize > 0) { "The Xbox disc image has an empty root directory." }

        val layout = readLayout(reader, imageSize, partition, rootSector, rootSize)
        var nextSector = FIRST_DATA_SECTOR
        layout.directories.forEach { directory ->
            directory.newSector = nextSector
            nextSector += sectorsFor(directory.table.size.toLong())
        }
        layout.files.forEach { file ->
            file.newSector = nextSector
            nextSector += sectorsFor(file.size)
        }
        layout.patches.forEach { patch ->
            patch.table.putU32(patch.entryOffset + 4, patch.newSector)
        }
        header.putU32(20, layout.directories.first().newSector)

        val totalBytes = roundUp(nextSector * SECTOR_SIZE, IMAGE_MODULUS)
        val writer = SectorWriter(output, totalBytes, onProgress)
        writer.zeros(HEADER_SECTOR * SECTOR_SIZE)
        writer.write(header, header.size)
        writer.zeros(FIRST_DATA_SECTOR * SECTOR_SIZE - writer.written)
        layout.directories.forEach { directory ->
            writer.write(directory.table, directory.table.size)
            val tableEnd = (directory.newSector + sectorsFor(directory.table.size.toLong())) * SECTOR_SIZE
            writer.fill(tableEnd - writer.written, 0xFF.toByte())
        }
        val buffer = ByteArray(COPY_BUFFER_BYTES)
        layout.files.forEach { file ->
            var position = partition + file.sourceSector * SECTOR_SIZE
            var remaining = file.size
            while (remaining > 0) {
                ensureActive()
                val chunk = minOf(remaining, buffer.size.toLong()).toInt()
                reader.readFully(position, buffer, 0, chunk)
                writer.write(buffer, chunk)
                position += chunk
                remaining -= chunk
            }
            writer.zeros((file.newSector + sectorsFor(file.size)) * SECTOR_SIZE - writer.written)
        }
        writer.zeros(totalBytes - writer.written)
        output.flush()
        return writer.written
    }

    private class Directory(val table: ByteArray) {
        var newSector = 0L
    }

    private class FileExtent(val sourceSector: Long, val size: Long) {
        var newSector = 0L
    }

    /** A directory entry whose start sector must point at its relocated directory or file. */
    private class SectorPatch(
        val table: ByteArray,
        val entryOffset: Int,
        val directory: Directory? = null,
        val file: FileExtent? = null,
    ) {
        val newSector: Long get() = directory?.newSector ?: file?.newSector ?: 0L
    }

    private class Layout(
        val directories: List<Directory>,
        val files: List<FileExtent>,
        val patches: List<SectorPatch>,
    )

    private fun readLayout(
        reader: ImageReader,
        imageSize: Long,
        partition: Long,
        rootSector: Long,
        rootSize: Long,
    ): Layout {
        val directories = mutableListOf<Directory>()
        val files = linkedMapOf<Pair<Long, Long>, FileExtent>()
        val patches = mutableListOf<SectorPatch>()
        val pending = ArrayDeque<Pair<Long, Long>>()
        val tables = mutableMapOf<Long, Directory>()

        fun directoryAt(sector: Long, size: Long): Directory = tables.getOrPut(sector) {
            require(size <= MAX_DIRECTORY_TABLE_BYTES) { "The Xbox disc image has an oversized directory table." }
            val position = partition + sector * SECTOR_SIZE
            require(position + size <= imageSize) { "The Xbox disc image is truncated or corrupt." }
            val table = ByteArray(size.toInt())
            reader.readFully(position, table, 0, table.size)
            Directory(table).also {
                directories += it
                pending += sector to size
            }
        }

        directoryAt(rootSector, rootSize)
        while (pending.isNotEmpty()) {
            val (sector, _) = pending.removeFirst()
            val table = tables.getValue(sector).table
            val visited = mutableSetOf<Int>()
            val nodes = ArrayDeque(listOf(0))
            while (nodes.isNotEmpty()) {
                val offset = nodes.removeLast()
                if (!visited.add(offset)) continue
                require(offset + ENTRY_HEADER_SIZE <= table.size) { "The Xbox disc image has a corrupt directory entry." }
                val left = table.u16(offset)
                val right = table.u16(offset + 2)
                if (left != 0 && left != NO_CHILD) nodes += left * 4
                if (right != 0 && right != NO_CHILD) nodes += right * 4
                val entrySector = table.u32(offset + 4)
                val entrySize = table.u32(offset + 8)
                val isDirectory = table[offset + 12].toInt() and DIRECTORY_ATTRIBUTE != 0
                patches += when {
                    entrySize == 0L -> SectorPatch(table, offset)
                    isDirectory -> SectorPatch(table, offset, directory = directoryAt(entrySector, entrySize))
                    else -> {
                        require(partition + entrySector * SECTOR_SIZE + entrySize <= imageSize) {
                            "The Xbox disc image is truncated or corrupt."
                        }
                        SectorPatch(table, offset, file = files.getOrPut(entrySector to entrySize) { FileExtent(entrySector, entrySize) })
                    }
                }
            }
        }
        return Layout(directories, files.values.toList(), patches)
    }

    private class SectorWriter(
        private val output: OutputStream,
        private val totalBytes: Long,
        private val onProgress: (Float) -> Unit,
    ) {
        var written = 0L
            private set
        private var reportedPercent = -1

        fun write(bytes: ByteArray, length: Int) {
            output.write(bytes, 0, length)
            advance(length.toLong())
        }

        fun zeros(count: Long) = fill(count, 0)

        fun fill(count: Long, value: Byte) {
            if (count <= 0) return
            val block = ByteArray(minOf(count, COPY_BUFFER_BYTES.toLong()).toInt()) { value }
            var remaining = count
            while (remaining > 0) {
                val chunk = minOf(remaining, block.size.toLong()).toInt()
                write(block, chunk)
                remaining -= chunk
            }
        }

        private fun advance(count: Long) {
            written += count
            val percent = (written * 100 / totalBytes).toInt()
            if (percent != reportedPercent) {
                reportedPercent = percent
                onProgress((percent / 100f).coerceIn(0f, 1f))
            }
        }
    }

    private fun readHeader(reader: ImageReader, partition: Long): ByteArray =
        ByteArray(SECTOR_SIZE.toInt()).also {
            reader.readFully(partition + HEADER_SECTOR * SECTOR_SIZE, it, 0, it.size)
        }

    private fun hasMagic(header: ByteArray): Boolean =
        header.matchesAt(0, MAGIC) && header.matchesAt(TRAILING_MAGIC_OFFSET, MAGIC)

    private fun ByteArray.matchesAt(offset: Int, expected: ByteArray): Boolean =
        expected.indices.all { this[offset + it] == expected[it] }

    private fun sectorsFor(bytes: Long): Long = (bytes + SECTOR_SIZE - 1) / SECTOR_SIZE

    private fun roundUp(value: Long, modulus: Long): Long = (value + modulus - 1) / modulus * modulus

    private fun ByteArray.u16(offset: Int): Int =
        (this[offset].toInt() and 0xFF) or ((this[offset + 1].toInt() and 0xFF) shl 8)

    private fun ByteArray.u32(offset: Int): Long =
        (u16(offset).toLong()) or (u16(offset + 2).toLong() shl 16)

    private fun ByteArray.putU32(offset: Int, value: Long) {
        for (index in 0 until 4) this[offset + index] = (value shr (8 * index)).toByte()
    }
}
