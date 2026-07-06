package pup.app.mimir.domain

import java.nio.ByteBuffer
import java.nio.ByteOrder

object ParamSfoParser {
    fun parse(bytes: ByteArray): Map<String, Any> {
        require(bytes.size >= 20) { "param.sfo is too small." }

        val header = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        require(header.int == PSF_MAGIC) { "Invalid param.sfo header." }
        header.int // version
        val keyTableStart = header.int
        val dataTableStart = header.int
        val entryCount = header.int

        require(keyTableStart in 0 until bytes.size) { "Invalid key table offset." }
        require(dataTableStart in 0 until bytes.size) { "Invalid data table offset." }

        val values = linkedMapOf<String, Any>()
        repeat(entryCount) { index ->
            val entryOffset = 20 + index * 16
            require(entryOffset + 16 <= bytes.size) { "Truncated param.sfo index table." }
            val entry = ByteBuffer.wrap(bytes, entryOffset, 16).order(ByteOrder.LITTLE_ENDIAN)
            val keyOffset = entry.short.toInt() and 0xFFFF
            val format = entry.short.toInt() and 0xFFFF
            val dataLength = entry.int
            entry.int // max length, not needed
            val dataOffset = entry.int

            val key = readCString(bytes, keyTableStart + keyOffset)
            val valueStart = dataTableStart + dataOffset
            require(valueStart in 0 until bytes.size) { "Invalid value offset for key $key." }
            require(valueStart + dataLength <= bytes.size) { "Truncated value for key $key." }

            values[key] = when (format) {
                FORMAT_UTF8, FORMAT_UTF8_NULL -> {
                    bytes.copyOfRange(valueStart, valueStart + dataLength)
                        .toString(Charsets.UTF_8)
                        .trimEnd('\u0000')
                }

                FORMAT_INT32 -> ByteBuffer
                    .wrap(bytes, valueStart, minOf(4, dataLength))
                    .order(ByteOrder.LITTLE_ENDIAN)
                    .int

                else -> bytes.copyOfRange(valueStart, valueStart + dataLength)
            }
        }

        return values
    }

    private fun readCString(bytes: ByteArray, startIndex: Int): String {
        require(startIndex in 0 until bytes.size) { "Invalid key offset." }
        var endIndex = startIndex
        while (endIndex < bytes.size && bytes[endIndex] != 0.toByte()) {
            endIndex++
        }
        return bytes.copyOfRange(startIndex, endIndex).toString(Charsets.UTF_8)
    }

    private const val PSF_MAGIC = 0x46535000
    private const val FORMAT_UTF8 = 0x0004
    private const val FORMAT_UTF8_NULL = 0x0204
    private const val FORMAT_INT32 = 0x0404
}
