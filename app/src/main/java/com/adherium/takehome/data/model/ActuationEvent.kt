package com.adherium.takehome.data.model

import java.time.Instant

data class ActuationEvent(
    val id: String,
    val timestamp: Instant,
    val deviceId: String,
    val puffs: Int
)
