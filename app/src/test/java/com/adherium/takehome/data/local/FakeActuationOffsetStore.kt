package com.adherium.takehome.data.local

class FakeActuationOffsetStore : ActuationOffsetStore {

    private var offset: Int = 0

    override suspend fun read(): Int = offset

    override suspend fun write(offset: Int) {
        this.offset = offset
    }
}
