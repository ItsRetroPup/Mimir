package pup.app.mimir.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ConverterProgressTest {
    @Test
    fun readsCompletionRatherThanCompressionRatio() {
        assertEquals(0.374f, ConverterProgress.parse("Compressing, 37.4% complete... (ratio=48.2%)")!!, 0.0001f)
    }

    @Test
    fun fallsBackToFirstPercentage() {
        assertEquals(0.5f, ConverterProgress.parse("Converting 50% (block 12 of 24, 3%)")!!, 0.0001f)
        assertNull(ConverterProgress.parse("Input file: game.iso"))
    }

    @Test
    fun neverReportsProgressMovingBackwards() {
        val reported = mutableListOf<Float>()
        val output = listOf(
            "Compressing, 10.0% complete... (ratio=70.0%)",
            "Compressing, 25.0% complete... (ratio=40.0%)",
            "Compressing, 20.0% complete... (ratio=55.0%)",
            "Compressing, 25.0% complete... (ratio=52.0%)",
            "Compressing, 60.0% complete... (ratio=48.0%)",
        ).joinToString("\r")

        readConverterOutput(output.byteInputStream(), reported::add)

        assertEquals(listOf(0.1f, 0.25f, 0.6f), reported)
    }
}
