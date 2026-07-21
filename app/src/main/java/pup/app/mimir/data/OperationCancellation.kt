package pup.app.mimir.data

import java.util.concurrent.atomic.AtomicReference

enum class StopRequest {
    None,
    AfterCurrent,
    Now,
}

/** Cooperative cancellation shared by scanning, planning, and the native CHDMan process. */
class OperationCancellation {
    private val request = AtomicReference(StopRequest.None)

    fun stopNow() {
        request.set(StopRequest.Now)
    }

    fun stopAfterCurrent() {
        request.compareAndSet(StopRequest.None, StopRequest.AfterCurrent)
    }

    fun requestedStop(): StopRequest = request.get()

    fun shouldStopBeforeNextOperation(): Boolean = request.get() != StopRequest.None

    fun shouldInterruptCurrentOperation(): Boolean = request.get() == StopRequest.Now
}

class OperationStoppedException(message: String = "Operation stopped by user.") : RuntimeException(message)

data class ExecutionResult(
    val completedOperations: Int,
    val stopped: Boolean,
)
