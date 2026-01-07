package com.adherium.takehome.data.local

import com.adherium.takehome.data.model.ActuationEvent

interface ActuationEventDao {
    /** Inserts a list of events.
     * if event exist, ignore */
    suspend fun insert(events: List<ActuationEvent>)
}
