package pup.app.mimir.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class ParamSfoParserTest {
    @Test
    fun parsesUtf8StringFields() {
        val bytes = buildParamSfo(
            "TITLE_ID" to "PCSE00890",
            "TITLE" to "10 Second Ninja X",
        )

        val parsed = ParamSfoParser.parse(bytes)

        assertEquals("PCSE00890", parsed["TITLE_ID"])
        assertEquals("10 Second Ninja X", parsed["TITLE"])
    }

    private fun buildParamSfo(vararg fields: Pair<String, String>): ByteArray {
        val keyStream = ByteArrayOutputStream()
        val dataStream = ByteArrayOutputStream()
        val entries = ArrayList<ByteArray>(fields.size)

        fields.forEach { (key, value) ->
            val keyOffset = keyStream.size()
            keyStream.write(key.toByteArray(Charsets.UTF_8))
            keyStream.write(0)

            val dataOffset = dataStream.size()
            val valueBytes = (value + "\u0000").toByteArray(Charsets.UTF_8)
            dataStream.write(valueBytes)

            val entry = ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN)
            entry.putShort(keyOffset.toShort())
            entry.putShort(0x0204.toShort())
            entry.putInt(valueBytes.size)
            entry.putInt(valueBytes.size)
            entry.putInt(dataOffset)
            entries += entry.array()
        }

        val keyBytes = keyStream.toByteArray()
        val dataBytes = dataStream.toByteArray()
        val keyTableStart = 20 + entries.sumOf { it.size }
        val dataTableStart = keyTableStart + keyBytes.size

        val output = ByteArrayOutputStream()
        val header = ByteBuffer.allocate(20).order(ByteOrder.LITTLE_ENDIAN)
        header.putInt(0x46535000)
        header.putInt(0x00000101)
        header.putInt(keyTableStart)
        header.putInt(dataTableStart)
        header.putInt(fields.size)

        output.write(header.array())
        entries.forEach(output::write)
        output.write(keyBytes)
        output.write(dataBytes)
        return output.toByteArray()
    }
}
