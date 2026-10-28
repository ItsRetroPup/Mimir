package pup.app.mimir.domain

import java.io.ByteArrayOutputStream
import java.util.zip.CRC32
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Test

class RomPatcherTest {
    private val rom = ByteArray(64) { it.toByte() }

    @Test
    fun detectsFormatsFromSignature() {
        assertEquals(PatchFormat.Ips, RomPatcher.detect("PATCH".toByteArray()))
        assertEquals(PatchFormat.Ups, RomPatcher.detect("UPS1x".toByteArray()))
        assertEquals(PatchFormat.Bps, RomPatcher.detect("BPS1x".toByteArray()))
        assertNull(RomPatcher.detect("xdelta".toByteArray()))
        assertNull(RomPatcher.detect(ByteArray(2)))
    }

    @Test
    fun appliesIpsRecordsAndRuns() {
        val patch = ips {
            record(offset = 2, byteArrayOf(0x55, 0x66))
            run(offset = 10, length = 4, value = 0x7F)
        }

        val result = RomPatcher.apply(rom, patch)

        val expected = rom.copyOf().also {
            it[2] = 0x55
            it[3] = 0x66
            it.fill(0x7F, 10, 14)
        }
        assertArrayEquals(expected, result)
    }

    @Test
    fun ipsCanGrowTheRom() {
        val patch = ips { record(offset = 70, byteArrayOf(1, 2)) }

        val result = RomPatcher.apply(rom, patch)

        assertEquals(72, result.size)
        assertEquals(0, result[65].toInt())
        assertEquals(2, result[71].toInt())
    }

    @Test
    fun ipsTruncationExtensionShrinksTheRom() {
        val patch = ips(truncateTo = 16) { record(offset = 0, byteArrayOf(9)) }

        val result = RomPatcher.apply(rom, patch)

        assertEquals(16, result.size)
        assertEquals(9, result[0].toInt())
    }

    @Test
    fun ipsWithoutEndMarkerIsMalformed() {
        val patch = "PATCH".toByteArray() + byteArrayOf(0, 0, 1, 0, 4, 1)

        assertFailure(PatchFailure.Malformed) { RomPatcher.apply(rom, patch) }
    }

    @Test
    fun appliesUpsPatch() {
        val target = rom.copyOf(80).also {
            it[0] = 0x40
            it[30] = 0x41
            it[31] = 0x42
            it[79] = 0x43
        }

        val result = RomPatcher.apply(rom, ups(rom, target))

        assertArrayEquals(target, result)
    }

    @Test
    fun upsRejectsTheWrongRom() {
        val other = rom.copyOf().also { it[5] = 99 }
        val patch = ups(rom, rom.copyOf().also { it[0] = 1 })

        assertFailure(PatchFailure.WrongRom) { RomPatcher.apply(other, patch) }
    }

    @Test
    fun appliesEveryBpsCommand() {
        // Target: the first 8 source bytes, two new bytes, those two repeated twice more by a
        // TargetCopy that overlaps what it writes, then source bytes 40..43.
        val target = rom.copyOfRange(0, 8) + byteArrayOf(0x70, 0x71, 0x70, 0x71, 0x70, 0x71) +
            rom.copyOfRange(40, 44)
        val patch = bps(rom, target) {
            sourceRead(8)
            targetRead(byteArrayOf(0x70, 0x71))
            targetCopy(length = 4, relative = 8)
            sourceCopy(length = 4, relative = 40)
        }

        val result = RomPatcher.apply(rom, patch)

        assertArrayEquals(target, result)
    }

    @Test
    fun bpsRejectsTheWrongRom() {
        val target = rom.copyOf().also { it[0] = 1 }
        val patch = bps(rom, target) {
            targetRead(byteArrayOf(1))
            sourceRead(63)
        }

        assertFailure(PatchFailure.WrongRom) { RomPatcher.apply(rom.copyOf().also { it[9] = 0 }, patch) }
    }

    @Test
    fun bpsRetriesWithoutSnesCopierHeader() {
        // Headered SNES dumps are a multiple of 1 KiB plus the 512-byte header.
        val unheadered = ByteArray(1024) { it.toByte() }
        val target = unheadered.copyOf().also { it[0] = 1 }
        val patch = bps(unheadered, target) {
            targetRead(byteArrayOf(1))
            sourceRead(1023)
        }

        val result = RomPatcher.apply(ByteArray(512) { 0x33 } + unheadered, patch)

        assertArrayEquals(target, result)
    }

    @Test
    fun damagedPatchIsReportedBeforeApplying() {
        val patch = bps(rom, rom) { sourceRead(64) }
        patch[6] = (patch[6] + 1).toByte()

        assertFailure(PatchFailure.PatchDamaged) { RomPatcher.apply(rom, patch) }
    }

    @Test
    fun resultChecksumIsVerified() {
        val patch = bps(rom, rom, targetCrcOverride = 0x1234L) { sourceRead(64) }

        assertFailure(PatchFailure.ResultMismatch) { RomPatcher.apply(rom, patch) }
    }

    @Test
    fun unknownFilesAreRejected() {
        assertFailure(PatchFailure.UnknownFormat) { RomPatcher.apply(rom, "not a patch".toByteArray()) }
    }

    @Test
    fun namesOutputAfterThePatch() {
        assertEquals(
            "Super Mario World (Kaizo).sfc",
            RomPatcher.outputFileName("Super Mario World (USA).sfc", "Super Mario World (Kaizo).bps"),
        )
    }

    @Test
    fun neverReusesTheRomsOwnName() {
        assertEquals("Metroid (patched).nes", RomPatcher.outputFileName("Metroid.nes", "metroid.ips"))
        assertEquals("Metroid (patched).nes", RomPatcher.outputFileName("Metroid.nes", ".ips"))
        assertEquals("Hack", RomPatcher.outputFileName("Metroid", "Hack.ups"))
    }

    private fun assertFailure(expected: PatchFailure, block: () -> Unit) {
        try {
            block()
            fail("Expected $expected")
        } catch (error: PatchException) {
            assertEquals(expected, error.failure)
        }
    }

    private class IpsBuilder {
        val out = ByteArrayOutputStream().apply { write("PATCH".toByteArray()) }

        fun record(offset: Int, data: ByteArray) {
            out.write(bigEndian(offset, 3))
            out.write(bigEndian(data.size, 2))
            out.write(data)
        }

        fun run(offset: Int, length: Int, value: Int) {
            out.write(bigEndian(offset, 3))
            out.write(bigEndian(0, 2))
            out.write(bigEndian(length, 2))
            out.write(value)
        }
    }

    private fun ips(truncateTo: Int? = null, build: IpsBuilder.() -> Unit): ByteArray {
        val builder = IpsBuilder().apply(build)
        builder.out.write("EOF".toByteArray())
        truncateTo?.let { builder.out.write(bigEndian(it, 3)) }
        return builder.out.toByteArray()
    }

    private fun ups(source: ByteArray, target: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        out.write("UPS1".toByteArray())
        out.write(varInt(source.size.toLong()))
        out.write(varInt(target.size.toLong()))
        val length = maxOf(source.size, target.size)
        fun mask(i: Int): Int = ((source.getOrElse(i) { 0 }.toInt() xor target.getOrElse(i) { 0 }.toInt()) and 0xFF)
        var last = 0
        var i = 0
        while (i < length) {
            if (mask(i) == 0) {
                i++
                continue
            }
            out.write(varInt((i - last).toLong()))
            while (i < length && mask(i) != 0) out.write(mask(i++))
            out.write(0)
            i++
            last = i
        }
        return withFooter(out, crc(source), crc(target))
    }

    private class BpsBuilder {
        val out = ByteArrayOutputStream()
        private var sourceRelative = 0L
        private var targetRelative = 0L

        fun sourceRead(length: Int) = out.write(varInt(((length - 1L) shl 2) or 0))

        fun targetRead(data: ByteArray) {
            out.write(varInt(((data.size - 1L) shl 2) or 1))
            out.write(data)
        }

        fun sourceCopy(length: Int, relative: Long) {
            out.write(varInt(((length - 1L) shl 2) or 2))
            out.write(signed(relative - sourceRelative))
            sourceRelative = relative + length
        }

        fun targetCopy(length: Int, relative: Long) {
            out.write(varInt(((length - 1L) shl 2) or 3))
            out.write(signed(relative - targetRelative))
            targetRelative = relative + length
        }

        private fun signed(delta: Long) = varInt((kotlin.math.abs(delta) shl 1) or (if (delta < 0) 1 else 0))
    }

    private fun bps(
        source: ByteArray,
        target: ByteArray,
        targetCrcOverride: Long? = null,
        build: BpsBuilder.() -> Unit,
    ): ByteArray {
        val out = ByteArrayOutputStream()
        out.write("BPS1".toByteArray())
        out.write(varInt(source.size.toLong()))
        out.write(varInt(target.size.toLong()))
        out.write(varInt(0))
        out.write(BpsBuilder().apply(build).out.toByteArray())
        return withFooter(out, crc(source), targetCrcOverride ?: crc(target))
    }

    private fun withFooter(out: ByteArrayOutputStream, sourceCrc: Long, targetCrc: Long): ByteArray {
        out.write(littleEndian(sourceCrc))
        out.write(littleEndian(targetCrc))
        val body = out.toByteArray()
        return body + littleEndian(CRC32().apply { update(body) }.value)
    }

    private companion object {
        fun crc(bytes: ByteArray): Long = CRC32().apply { update(bytes) }.value

        fun bigEndian(value: Int, bytes: Int) = ByteArray(bytes) { i -> (value shr (8 * (bytes - 1 - i))).toByte() }

        fun littleEndian(value: Long) = ByteArray(4) { i -> (value shr (8 * i)).toByte() }

        fun varInt(value: Long): ByteArray {
            val out = ByteArrayOutputStream()
            var remaining = value
            while (true) {
                val low = (remaining and 0x7F).toInt()
                remaining = remaining shr 7
                if (remaining == 0L) {
                    out.write(0x80 or low)
                    return out.toByteArray()
                }
                out.write(low)
                remaining--
            }
        }
    }
}
