package com.adherium.takehome.domain

import com.adherium.takehome.data.ActuationEventRepo
import com.adherium.takehome.data.ActuationEventRepoImpl
import com.adherium.takehome.data.local.FakeActuationEventDao
import com.adherium.takehome.data.local.FakeActuationOffsetStore
import com.adherium.takehome.data.model.ActuationEvent
import com.adherium.takehome.data.remote.ble.FakeGattConnection
import com.adherium.takehome.data.remote.ble.FakeHailieSensor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class SyncManagerTest {

    companion object {
        private const val FAKE_DEVICE_ID = "test_device"
    }
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var fakeConnection: FakeGattConnection
    private lateinit var fakeSensor: FakeHailieSensor
    private lateinit var fakeDao: FakeActuationEventDao
    private lateinit var fakeOffsetStore: FakeActuationOffsetStore
    private lateinit var actuationEventRepo: ActuationEventRepo
    private lateinit var syncManager: DefaultSyncManager

    @Before
    fun setUp() {
        fakeConnection = FakeGattConnection()
        fakeSensor = FakeHailieSensor(fakeConnection)
        fakeDao = FakeActuationEventDao()
        fakeOffsetStore = FakeActuationOffsetStore()

        actuationEventRepo = ActuationEventRepoImpl(
            sensor = fakeSensor,
            dao = fakeDao,
            offsetStore = fakeOffsetStore,
            ioDispatcher = testDispatcher
        )

        syncManager = DefaultSyncManager(
            scope = testScope,
            actuationEventRepo = actuationEventRepo
        )
    }

    @After
    fun tearDown() {
        testScope.cancel()
    }

    @Test
    fun sync_completesSuccessfully() = runTest(testDispatcher) {
        // Setup device with events
        val events = (0 until 25).map {
            createActuationEvent(it)
        }

        // Preload events on the fake device
        fakeConnection.setEvents(events)

        // Act
        syncManager.startSync()
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        assert(syncManager.syncState.value is SyncState.Completed)
        assert(fakeDao.getAll().size == events.size)
        assert(fakeOffsetStore.read() == events.size)
    }

    @Test
    fun sync_withNoEvents_completesImmediately() = runTest(testDispatcher) {
        // Act
        syncManager.startSync()
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        assert(syncManager.syncState.value is SyncState.Completed)
        assert(fakeDao.getAll().isEmpty())
        assert(fakeOffsetStore.read() == 0)
    }

    @Test
    fun startSync_calledTwice_onlyOneSyncRuns() = runTest(testDispatcher) {
        // Setup device
        val events = (0 until 10).map { createActuationEvent(it) }
        fakeConnection.setEvents(events)

        // Act: trigger sync twice before it runs
        syncManager.startSync()
        syncManager.startSync()
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        assert(syncManager.syncState.value is SyncState.Completed)
        assert(fakeDao.getAll().size == events.size)
        assert(fakeOffsetStore.read() == events.size)
    }

    @Test
    fun sync_retriesOnTransientFailure_andSucceeds() = runTest(testDispatcher) {
        // Arrange
        val events = (0 until 15).map { createActuationEvent(it) }
        fakeConnection.setEvents(events)
        // Fail the first read to force a retry
        fakeConnection.failNextRead()

        // Act
        syncManager.startSync()
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        assert(syncManager.syncState.value is SyncState.Completed)
        assert(fakeDao.getAll().size == events.size)
        assert(fakeOffsetStore.read() == events.size)
    }

    @Test
    fun sync_recoversFromMidSyncConnectionFailure() = runTest(testDispatcher) {
        // Arrange: multiple pages of events
        val events = (0 until 30).map { createActuationEvent(it) }
        fakeConnection.setEvents(events)
        // Fail on the second read (mid-sync)
        fakeConnection.failOnRead(2)

        // Act
        syncManager.startSync()
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        assert(syncManager.syncState.value is SyncState.Completed)
        assert(fakeDao.getAll().size == events.size)
        assert(fakeOffsetStore.read() == events.size)
    }

    @Test
    fun sync_failsOnPersistenceError() = runTest(testDispatcher) {
        val events = (0 until 10).map { createActuationEvent(it) }
        fakeConnection.setEvents(events)

        fakeDao.failNextInsert()

        syncManager.startSync()
        testDispatcher.scheduler.advanceUntilIdle()

        assert(syncManager.syncState.value is SyncState.Failed)
        assert(fakeOffsetStore.read() == 0)
    }

    @Test
    fun sync_failsAfterMaxRetries() = runTest(testDispatcher) {
        // Arrange
        val events = (0 until 10).map { createActuationEvent(it) }
        fakeConnection.setEvents(events)

        // Always fail reads so every attempt fails
        fakeConnection.failOnEveryRead()

        // Act
        syncManager.startSync()
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        val state = syncManager.syncState.value
        assert(state is SyncState.Failed)
        assert(fakeDao.getAll().isEmpty())
        assert(fakeOffsetStore.read() == 0)
    }

    @Test
    fun sync_timesOutAfterPartialProgress_andFails() = runTest(testDispatcher) {
        // Arrange: enough events for multiple pages
        val events = (0 until 20).map { createActuationEvent(it) }
        fakeConnection.setEvents(events)

        // Timeout on second read (after first page)
        fakeConnection.timeoutOnRead(2)

        // Act
        syncManager.startSync()
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        val state = syncManager.syncState.value
        assert(state is SyncState.Failed)

        // First page should be persisted
        val persistedCount = fakeDao.getAll().size
        assert(persistedCount > 0)
        assert(persistedCount < events.size)

        // Offset should reflect partial progress
        assert(fakeOffsetStore.read() == persistedCount)
    }

    private fun createActuationEvent(
        index: Int,
        deviceId: String = FAKE_DEVICE_ID,
        puffs: Int = (1..20).random()
    ): ActuationEvent =
        ActuationEvent(
            id = "event-$index",
            timestamp = Instant.EPOCH.plusSeconds(index.toLong()),
            deviceId = deviceId,
            puffs = puffs
        )
}
