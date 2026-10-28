package pup.app.mimir.domain

import java.util.zip.CRC32

/** The ROM hacking patch formats Mimir can apply. */
enum class PatchFormat(val displayName: String) {
    Ips("IPS"),
    Ups("UPS"),
    Bps("BPS"),
}

/** Why a patch could not be applied; each maps to a plain-language message in the UI. */
enum class PatchFailure {
    /** The file is not an IPS, UPS, or BPS patch. */
    UnknownFormat,

    /** The patch ends early or its records point outside the file. */
    Malformed,

    /** The patch's own checksum does not match, so it was damaged in download or copy. */
    PatchDamaged,

    /** The patch was made for a different ROM (another region, revision, or a headered dump). */
    WrongRom,

    /** The patch applied, but the result does not match the checksum the patch expects. */
    ResultMismatch,

    /** The ROM is too large to patch in the memory available. */
    TooLarge,
}

class PatchException(val failure: PatchFailure) : Exception(failure.name)

/**
 * Applies IPS, UPS, and BPS patches in memory. UPS and BPS carry CRC32 checksums of the source
 * ROM, the patched result, and the patch itself; all three are verified.
 */
object RomPatcher {
    private val IPS_MAGIC = "PATCH".toByteArray(Charsets.US_ASCII)
    private val IPS_EOF = "EOF".toByteArray(Charsets.US_ASCII)
    private val UPS_MAGIC = "UPS1".toByteArray(Charsets.US_ASCII)
    private val BPS_MAGIC = "BPS1".toByteArray(Charsets.US_ASCII)
    private const val FOOTER_SIZE = 12
    private const val COPIER_HEADER_SIZE = 512

    /** Bytes needed from the start of a file to recognise its format. */
    const val SIGNATURE_SIZE = 5

    /** Recognises a patch from its first [SIGNATURE_SIZE] bytes, or returns null. */
    fun detect(header: ByteArray): PatchFormat? = when {
        header.startsWith(IPS_MAGIC) -> PatchFormat.Ips
        header.startsWith(UPS_MAGIC) -> PatchFormat.Ups
        header.startsWith(BPS_MAGIC) -> PatchFormat.Bps
        else -> null
    }

    fun apply(source: ByteArray, patch: ByteArray): ByteArray = when (detect(patch)) {
        PatchFormat.Ips -> applyIps(source, patch)
        PatchFormat.Ups -> withHeaderFallback(source) { applyUps(it, patch) }
        PatchFormat.Bps -> withHeaderFallback(source) { applyBps(it, patch) }
        null -> throw PatchException(PatchFailure.UnknownFormat)
    }

    /**
     * Names the patched copy after the patch, since that is usually the hack or translation's
     * name, keeping the ROM's extension. Falls back to "<rom> (patched)" if that would reuse the
     * ROM's own name, so the original is never overwritten.
     */
    fun outputFileName(romFileName: String, patchFileName: String): String {
        val romStem = romFileName.substringBeforeLast('.')
        val extension = romFileName.substringAfterLast('.', "").let { if (it.isEmpty()) "" else ".$it" }
        val patchStem = patchFileName.substringBeforeLast('.').trim()
        val preferred = "$patchStem$extension"
        return if (patchStem.isEmpty() || preferred.equals(romFileName, ignoreCase = true)) {
            "$romStem (patched)$extension"
        } else {
            preferred
        }
    }

    /**
     * SNES dumps often carry a 512-byte copier header that UPS and BPS patches were not made
     * against. When the ROM is rejected and has one, retry without it.
     */
    private inline fun withHeaderFallback(source: ByteArray, attempt: (ByteArray) -> ByteArray): ByteArray =
        try {
            attempt(source)
        } catch (error: PatchException) {
            if (error.failure != PatchFailure.WrongRom || source.size % 1024 != COPIER_HEADER_SIZE) throw error
            attempt(source.copyOfRange(COPIER_HEADER_SIZE, source.size))
        }

    private fun applyIps(source: ByteArray, patch: ByteArray): ByteArray {
        val reader = PatchReader(patch, IPS_MAGIC.size, patch.size)
        var output = source.copyOf()
        var size = source.size
        fun ensureSize(end: Int) {
            if (end > output.size) output = output.copyOf(maxOf(end, output.size * 2))
            if (end > size) size = end
        }
        while (true) {
            if (reader.remaining >= IPS_EOF.size && reader.peekEquals(IPS_EOF)) {
                reader.skip(IPS_EOF.size)
                break
            }
            val offset = reader.bigEndian(3)
            val length = reader.bigEndian(2)
            if (length == 0) {
                val runLength = reader.bigEndian(2)
                val value = reader.byte()
                ensureSize(offset + runLength)
                output.fill(value.toByte(), offset, offset + runLength)
            } else {
                ensureSize(offset + length)
                reader.copyTo(output, offset, length)
            }
        }
        // The optional truncation extension: three more bytes after EOF give the final size.
        if (reader.remaining >= 3) size = reader.bigEndian(3)
        return output.copyOf(size)
    }

    private fun applyUps(source: ByteArray, patch: ByteArray): ByteArray {
        verifyPatchChecksum(patch)
        val reader = PatchReader(patch, UPS_MAGIC.size, patch.size - FOOTER_SIZE)
        val sourceSize = reader.varInt()
        val targetSize = reader.varInt()
        val sourceCrc = patch.littleEndianInt(patch.size - 12)
        val targetCrc = patch.littleEndianInt(patch.size - 8)
        // UPS is symmetric, but Mimir only patches forward: the input must be the original.
        if (source.size.toLong() != sourceSize || crc32(source) != sourceCrc) {
            throw PatchException(PatchFailure.WrongRom)
        }
        val output = source.copyOf(checkedSize(targetSize))
        var position = 0L
        while (reader.remaining > 0) {
            position += reader.varInt()
            while (true) {
                val mask = reader.byte()
                if (position < output.size) {
                    val original = if (position < source.size) source[position.toInt()].toInt() else 0
                    output[position.toInt()] = (original xor mask).toByte()
                }
                position++
                if (mask == 0) break
            }
        }
        if (crc32(output) != targetCrc) throw PatchException(PatchFailure.ResultMismatch)
        return output
    }

    private fun applyBps(source: ByteArray, patch: ByteArray): ByteArray {
        verifyPatchChecksum(patch)
        val reader = PatchReader(patch, BPS_MAGIC.size, patch.size - FOOTER_SIZE)
        val sourceSize = reader.varInt()
        val targetSize = reader.varInt()
        reader.skip(checkedSize(reader.varInt()))
        val sourceCrc = patch.littleEndianInt(patch.size - 12)
        val targetCrc = patch.littleEndianInt(patch.size - 8)
        if (source.size.toLong() != sourceSize || crc32(source) != sourceCrc) {
            throw PatchException(PatchFailure.WrongRom)
        }
        val output = ByteArray(checkedSize(targetSize))
        var outputOffset = 0
        var sourceRelative = 0L
        var targetRelative = 0L
        while (reader.remaining > 0) {
            val command = reader.varInt()
            val length = checkedSize((command shr 2) + 1)
            if (outputOffset.toLong() + length > output.size) throw PatchException(PatchFailure.Malformed)
            when ((command and 3).toInt()) {
                SOURCE_READ -> {
                    if (outputOffset + length > source.size) throw PatchException(PatchFailure.Malformed)
                    source.copyInto(output, outputOffset, outputOffset, outputOffset + length)
                }
                TARGET_READ -> reader.copyTo(output, outputOffset, length)
                SOURCE_COPY -> {
                    sourceRelative += reader.signedVarInt()
                    if (sourceRelative < 0 || sourceRelative + length > source.size) throw PatchException(PatchFailure.Malformed)
                    source.copyInto(output, outputOffset, sourceRelative.toInt(), sourceRelative.toInt() + length)
                    sourceRelative += length
                }
                else -> {
                    targetRelative += reader.signedVarInt()
                    if (targetRelative < 0 || targetRelative >= outputOffset) throw PatchException(PatchFailure.Malformed)
                    // Byte by byte on purpose: a TargetCopy may overlap the bytes it is writing.
                    repeat(length) { i -> output[outputOffset + i] = output[(targetRelative + i).toInt()] }
                    targetRelative += length
                }
            }
            outputOffset += length
        }
        if (outputOffset != output.size) throw PatchException(PatchFailure.Malformed)
        if (crc32(output) != targetCrc) throw PatchException(PatchFailure.ResultMismatch)
        return output
    }

    private const val SOURCE_READ = 0
    private const val TARGET_READ = 1
    private const val SOURCE_COPY = 2

    private fun verifyPatchChecksum(patch: ByteArray) {
        if (patch.size < FOOTER_SIZE + SIGNATURE_SIZE) throw PatchException(PatchFailure.Malformed)
        val expected = patch.littleEndianInt(patch.size - 4)
        val crc = CRC32().apply { update(patch, 0, patch.size - 4) }.value
        if (crc != expected) throw PatchException(PatchFailure.PatchDamaged)
    }

    private fun checkedSize(size: Long): Int {
        if (size < 0 || size > Int.MAX_VALUE - 8) throw PatchException(PatchFailure.TooLarge)
        return size.toInt()
    }

    private fun crc32(bytes: ByteArray): Long = CRC32().apply { update(bytes) }.value

    private fun ByteArray.startsWith(prefix: ByteArray): Boolean =
        size >= prefix.size && prefix.indices.all { this[it] == prefix[it] }

    private fun ByteArray.littleEndianInt(offset: Int): Long =
        (0 until 4).fold(0L) { value, i -> value or ((this[offset + i].toLong() and 0xFF) shl (8 * i)) }

    /** Reads patch records between [start] and [end], failing with [PatchFailure.Malformed] past the end. */
    private class PatchReader(private val bytes: ByteArray, start: Int, private val end: Int) {
        private var position = start

        val remaining: Int get() = end - position

        fun byte(): Int {
            if (position >= end) throw PatchException(PatchFailure.Malformed)
            return bytes[position++].toInt() and 0xFF
        }

        fun skip(count: Int) {
            if (count > remaining) throw PatchException(PatchFailure.Malformed)
            position += count
        }

        fun peekEquals(expected: ByteArray): Boolean = expected.indices.all { bytes[position + it] == expected[it] }

        fun bigEndian(byteCount: Int): Int = (0 until byteCount).fold(0) { value, _ -> (value shl 8) or byte() }

        fun copyTo(target: ByteArray, offset: Int, length: Int) {
            if (length > remaining) throw PatchException(PatchFailure.Malformed)
            bytes.copyInto(target, offset, position, position + length)
            position += length
        }

        /** The variable-length integer shared by UPS and BPS. */
        fun varInt(): Long {
            var value = 0L
            var shift = 1L
            while (true) {
                val next = byte()
                value += (next and 0x7F) * shift
                if (next and 0x80 != 0) return value
                shift = shift shl 7
                value += shift
                if (shift > Long.MAX_VALUE shr 8) throw PatchException(PatchFailure.Malformed)
            }
        }

        /** A BPS relative offset: the low bit is the sign. */
        fun signedVarInt(): Long {
            val raw = varInt()
            val magnitude = raw shr 1
            return if (raw and 1L != 0L) -magnitude else magnitude
        }
    }
}
