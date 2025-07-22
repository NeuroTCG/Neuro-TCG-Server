import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SuspendingCyclicBarrier(
    private val parties: Int,
) {
    private val mutex = Mutex()
    private val waiters = mutableListOf<CompletableDeferred<Unit>>()

    suspend fun await() {
        val waiter = CompletableDeferred<Unit>()

        mutex.withLock {
            waiters.add(waiter)

            if (waiters.size == parties) {
                waiters.forEach { it.complete(Unit) }
                waiters.clear()
            }
        }

        waiter.await()
    }

    suspend fun reset() {
        mutex.withLock {
            waiters.forEach { it.cancel() }
            waiters.clear()
        }
    }
}
