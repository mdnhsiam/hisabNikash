package com.hisabnikash.app.backup

import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** One instance per app container. Callers must not acquire it recursively. */
class BackupOperationGate {
    private val mutex = Mutex()
    private val generation = AtomicLong()
    val version: Long get() = generation.get()

    suspend fun <T> run(operation: suspend () -> T): T = mutex.withLock { operation() }

    // Called while the gate is held when a destination is disabled or data is deleted.
    fun invalidateAuthorization() { generation.incrementAndGet() }
}
