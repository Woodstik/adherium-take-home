package com.adherium.takehome.data

import com.adherium.takehome.data.model.ActuationEvent

interface ActuationEventRepo {
    /** Ensures the device is ready for data transfer (bonded + connected). */
    suspend fun connect()

    /** Returns the total number of available events on the device. */
    suspend fun getEventCount(): Int

    /** Reads a page of events. */
    suspend fun readEvents(
        limit: Int
    ): List<ActuationEvent>

    /** Stores events */
    suspend fun persist(events: List<ActuationEvent>)

    /** Acknowledges [count] number of events. */
    suspend fun acknowledge(count: Int)

    /** Disconnects and cleans up resources */
    suspend fun disconnect()
}
