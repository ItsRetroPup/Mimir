package pup.app.mimir.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OperationCancellationTest {
    @Test
    fun stopAfterCurrentWaitsForTheCurrentOperation() {
        val cancellation = OperationCancellation()

        cancellation.stopAfterCurrent()

        assertTrue(cancellation.shouldStopBeforeNextOperation())
        assertFalse(cancellation.shouldInterruptCurrentOperation())
        assertEquals(StopRequest.AfterCurrent, cancellation.requestedStop())
    }

    @Test
    fun stopNowInterruptsTheCurrentOperation() {
        val cancellation = OperationCancellation()

        cancellation.stopNow()

        assertTrue(cancellation.shouldStopBeforeNextOperation())
        assertTrue(cancellation.shouldInterruptCurrentOperation())
        assertEquals(StopRequest.Now, cancellation.requestedStop())
    }
}
