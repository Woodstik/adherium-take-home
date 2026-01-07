package com.adherium.takehome.data.remote.ble

import com.adherium.takehome.data.model.ActuationEvent

interface GattConnection {
    suspend fun readEventCount(): Result<Int>
    suspend fun readEvents(offset: Int, count: Int): Result<List<ActuationEvent>>
    suspend fun acknowledgeEvents(upToOffset: Int): Result<Unit>
}
