package com.adherium.takehome.data.remote.ble

import kotlinx.coroutines.flow.StateFlow

interface HailieSensor {
    val bondState: StateFlow<BondState>
    val connectionState: StateFlow<ConnectionState>

    /**
     * Initiates device bonding if not already bonded.
     *
     * Expectations:
     * - Safe to call multiple times (idempotent)
     * - Returns success only when the device is bonded
     * - Returns failure if bonding cannot be initiated or fails
     */
    suspend fun bond(): Result<Unit>

    /**
     * Establishes a GATT connection or returns an active one.
     *
     * Expectations:
     * - Safe to call when already connected (idempotent)
     * - Returns a valid, ready-to-use GattConnection on success
     * - Returns failure if the connection cannot be established
     */
    suspend fun connect(): Result<GattConnection>

    /**
     * Disconnects from the device and releases resources.
     *
     * Expectations:
     * - Safe to call when not connected
     * - Best-effort cleanup; errors are not surfaced
     */
    suspend fun disconnect()
}
