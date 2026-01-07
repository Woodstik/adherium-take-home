package com.adherium.takehome.data.remote.ble

import com.adherium.takehome.data.model.ActuationEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakeHailieSensor(
    private val fakeConnection: FakeGattConnection
) : HailieSensor {

    private val _bondState = MutableStateFlow(BondState.NONE)
    override val bondState: StateFlow<BondState> = _bondState

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<ConnectionState> = _connectionState

    private var failNextBondCall = false
    private var failNextConnectCall = false

    private var activeConnection: FakeGattConnection? = null

    override suspend fun bond(): Result<Unit> {
        if (failNextBondCall) {
            failNextBondCall = false
            return Result.failure(Exception("Forced bond failure"))
        }
        if (_bondState.value == BondState.BONDED) {
            return Result.success(Unit)
        }
        _bondState.value = BondState.BONDED
        return Result.success(Unit)
    }

    override suspend fun connect(): Result<FakeGattConnection> {
        if (failNextConnectCall) {
            failNextConnectCall = false
            return Result.failure(Exception("Forced connect failure"))
        }
        if (_bondState.value != BondState.BONDED) {
            return Result.failure(Exception("Cannot connect when not bonded"))
        }
        if (_connectionState.value == ConnectionState.CONNECTED && activeConnection != null) {
            return Result.success(activeConnection!!)
        }
        _connectionState.value = ConnectionState.CONNECTED
        activeConnection = fakeConnection
        return Result.success(activeConnection!!)
    }

    override suspend fun disconnect() {
        _connectionState.value = ConnectionState.DISCONNECTED
        activeConnection = null
    }

    fun failNextBond() {
        failNextBondCall = true
    }

    fun failNextConnect() {
        failNextConnectCall = true
    }
}
