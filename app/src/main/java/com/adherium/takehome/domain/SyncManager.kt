package com.adherium.takehome.domain

import com.adherium.takehome.data.ActuationEventRepo
import com.adherium.takehome.data.model.ActuationEventRepoException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.pow

interface SyncManager {

    /** Current sync state, observable by multiple callers */
    val syncState: StateFlow<SyncState>

    /** Triggers a sync if one is not already running */
    suspend fun startSync()
}

class DefaultSyncManager(
    private val scope: CoroutineScope,
    private val actuationEventRepo: ActuationEventRepo,
) : SyncManager {

    companion object {
        private const val TAG = "SyncManager"

        private const val PAGE_SIZE = 10
        private const val MAX_RETRIES = 5
        private const val BASE_RETRY_DELAY_MS = 500L
        private const val MAX_RETRY_DELAY_MS = 5000L
    }

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    override val syncState = _syncState.asStateFlow()
    private val mutex = Mutex()
    private var syncJob: Job? = null

    override suspend fun startSync() = mutex.withLock {
        if (syncJob != null) return
        syncJob = scope.launch {
            try {
                syncWithRetries()
            } finally {
                syncJob = null
            }
        }
    }

    private suspend fun syncWithRetries() {
        repeat(MAX_RETRIES) { attempt ->
            try {
                syncOnce()
                _syncState.update { SyncState.Completed }
                return
            } catch (e: Throwable) {
                val retryable = isRetryable(e)
                if (!retryable || attempt == MAX_RETRIES - 1) {
                    _syncState.update { SyncState.Failed(e, retryable) }
                    return
                }
                delay(backoff(attempt))
            }
        }
    }

    private suspend fun syncOnce() {
        try {
            // Bind and connect to device
            _syncState.update { SyncState.Connecting }
            actuationEventRepo.connect()

            // Read event count
            _syncState.update { SyncState.ReadingCount }
            val eventCount = actuationEventRepo.getEventCount()
            if (eventCount == 0) {
                return
            }

            // Read events
            _syncState.update { SyncState.ReadingEvents }
            var hasMore = true
            while (hasMore) {
                val events = actuationEventRepo.readEvents(PAGE_SIZE)
                if (events.isEmpty()) {
                    hasMore = false
                } else {
                    actuationEventRepo.persist(events)
                    actuationEventRepo.acknowledge(events.size)
                }
            }
        } finally {
            actuationEventRepo.disconnect()
        }
    }

    private fun isRetryable(t: Throwable): Boolean {
        return when {
            t is ActuationEventRepoException &&
                    t !is ActuationEventRepoException.PersistenceException -> true

            else -> false
        }
    }

    private fun backoff(attempt: Int): Long {
        val baseDelayMs = BASE_RETRY_DELAY_MS
        val maxDelayMs = MAX_RETRY_DELAY_MS
        val multiplier = 2.0.pow(attempt.toDouble()).toLong()
        return (baseDelayMs * multiplier).coerceAtMost(maxDelayMs)
    }
}
