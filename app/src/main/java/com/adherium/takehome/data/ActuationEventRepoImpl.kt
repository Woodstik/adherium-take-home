package com.adherium.takehome.data

import com.adherium.takehome.data.local.ActuationEventDao
import com.adherium.takehome.data.local.ActuationOffsetStore
import com.adherium.takehome.data.model.ActuationEvent
import com.adherium.takehome.data.model.ActuationEventRepoException.AcknowledgeException
import com.adherium.takehome.data.model.ActuationEventRepoException.BondingException
import com.adherium.takehome.data.model.ActuationEventRepoException.ConnectionException
import com.adherium.takehome.data.model.ActuationEventRepoException.PersistenceException
import com.adherium.takehome.data.model.ActuationEventRepoException.ReadException
import com.adherium.takehome.data.remote.ble.BondState
import com.adherium.takehome.data.remote.ble.GattConnection
import com.adherium.takehome.data.remote.ble.HailieSensor
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

class ActuationEventRepoImpl(
    private val sensor: HailieSensor,
    private val dao: ActuationEventDao,
    private val offsetStore: ActuationOffsetStore,
    private val ioDispatcher: CoroutineDispatcher
) : ActuationEventRepo {

    companion object {
        private const val TAG = "ActuationEventRepoImpl"

        private const val BIND_TIMEOUT_MS = 60 * 1000L
        private const val CONNECT_TIMEOUT_MS = 30 * 1000L
        private const val READ_TIMEOUT_MS = 5 * 1000L
        private const val ACK_TIMEOUT_MS = 3 * 1000L
    }

    override suspend fun connect() {
        withContext(ioDispatcher) {
            requireBonded()
            requireConnected()
        }
    }

    override suspend fun getEventCount(): Int = withContext(ioDispatcher) {
        val connection = requireReady()
        return@withContext try {
            withTimeout(READ_TIMEOUT_MS) {
                connection.readEventCount().getOrThrow()
            }
        } catch (t: Throwable) {
            throw ReadException(t)
        }
    }

    override suspend fun readEvents(
        limit: Int
    ): List<ActuationEvent> = withContext(ioDispatcher) {
        val connection = requireReady()
        val lastOffset = offsetStore.read()
        return@withContext try {
            withTimeout(READ_TIMEOUT_MS) {
                connection.readEvents(lastOffset, limit).getOrThrow()
            }
        } catch (t: Throwable) {
            throw ReadException(t)
        }
    }

    override suspend fun persist(events: List<ActuationEvent>) = withContext(ioDispatcher) {
        try {
            dao.insert(events)
        } catch (t: Throwable) {
            throw PersistenceException(t)
        }
    }

    override suspend fun acknowledge(count: Int) = withContext(ioDispatcher) {
        val connection = requireReady()
        val lastOffset = offsetStore.read()
        val newOffset = lastOffset + count
        try {
            withTimeout(ACK_TIMEOUT_MS) {
                connection.acknowledgeEvents(newOffset).getOrThrow()
            }
            offsetStore.write(newOffset)
        } catch (t: Throwable) {
            throw AcknowledgeException(t)
        }
    }

    override suspend fun disconnect() {
        sensor.disconnect()
    }

    private suspend fun requireReady(): GattConnection {
        requireBonded()
        return requireConnected()
    }

    private suspend fun requireConnected(): GattConnection {
        try {
            // HailieSensor owns the connection lifecycle; connect() is expected to be idempotent
            return withTimeout(CONNECT_TIMEOUT_MS) {
                sensor.connect().getOrThrow()
            }
        } catch (t: Throwable) {
            throw ConnectionException(t)
        }
    }

    private suspend fun requireBonded() {
        if (sensor.bondState.value == BondState.BONDED) return
        try {
            withTimeout(BIND_TIMEOUT_MS) {
                sensor.bond().getOrThrow()
            }
        } catch (t: Throwable) {
            throw BondingException(t)
        }
    }
}
