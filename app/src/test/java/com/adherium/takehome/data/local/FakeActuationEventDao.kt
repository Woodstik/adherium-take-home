package com.adherium.takehome.data.local

import com.adherium.takehome.data.model.ActuationEvent

/**
 * In-memory fake implementation of ActuationEventDao used for unit tests.
 * Stores events in a map.
 */
class FakeActuationEventDao : ActuationEventDao {
    private val storage = mutableMapOf<String, ActuationEvent>()

    private var failNextInsert = false

    override suspend fun insert(events: List<ActuationEvent>) {
        if(failNextInsert) {
            failNextInsert = false
            throw Exception("Simulated insert failure")
        }

        events.forEach { event ->
            storage.putIfAbsent(event.id, event)
        }
    }

    fun getAll(): List<ActuationEvent> = storage.values.toList()

    fun failNextInsert() {
        failNextInsert = true
    }
}
