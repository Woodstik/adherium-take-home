package com.adherium.takehome.data.remote.ble

import com.adherium.takehome.data.model.ActuationEvent
import kotlinx.coroutines.delay

class FakeGattConnection : GattConnection {

    private val events = mutableListOf<ActuationEvent>()

    private var lastAckedOffset: Int = 0
    private var failNextAck = false

    private var readCount = 0
    private var failOnReadNumber: Int = -1
    private var failOnEveryRead: Boolean = false
    private var timeoutOnReadNumber: Int? = null
    fun setEvents(newEvents: List<ActuationEvent>) {
        events.clear()
        events.addAll(newEvents)
        lastAckedOffset = 0
    }

    fun failNextRead() {
        failOnReadNumber = 0
    }

    fun failOnRead(number: Int) {
        failOnReadNumber = number
    }

    fun failOnEveryRead() {
        failOnEveryRead = true
    }

    fun timeoutOnRead(number: Int) {
        timeoutOnReadNumber = number
    }

    fun failNextAck() {
        failNextAck = true
    }

    override suspend fun readEventCount(): Result<Int> {
        // Device always reports total count (absolute)
        return Result.success(events.size)
    }

    override suspend fun readEvents(
        offset: Int,
        count: Int
    ): Result<List<ActuationEvent>> {
        readCount++
        if (failOnEveryRead || failOnReadNumber == readCount) {
            return Result.failure(Exception("Simulated read failure on read #$readCount"))
        }

        if (timeoutOnReadNumber != null && timeoutOnReadNumber!! <= readCount ) {
            delay(Long.MAX_VALUE) // will trigger withTimeout
        }

        // Already acknowledged range → nothing to read
        if (offset < lastAckedOffset) {
            return Result.success(emptyList())
        }

        // Out of bounds
        if (offset < 0 || offset >= events.size) {
            return Result.success(emptyList())
        }

        val endIndex = (offset + count).coerceAtMost(events.size)
        return Result.success(events.subList(offset, endIndex).toList())
    }

    override suspend fun acknowledgeEvents(upToOffset: Int): Result<Unit> {
        if (failNextAck) {
            failNextAck = false
            return Result.failure(Exception("Simulated acknowledge failure"))
        }

        // ACK is monotonic and clamped
        val clamped = upToOffset.coerceAtMost(events.size)
        lastAckedOffset = maxOf(lastAckedOffset, clamped)

        return Result.success(Unit)
    }
}
