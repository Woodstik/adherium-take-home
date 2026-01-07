package com.adherium.takehome.data.local

interface ActuationOffsetStore {
    suspend fun read() : Int
    suspend fun write(offset: Int)
}
