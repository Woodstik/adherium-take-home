package com.adherium.takehome.domain


sealed interface SyncState {

    data object Idle : SyncState

    data object Connecting : SyncState

    data object ReadingCount : SyncState

    data object ReadingEvents : SyncState

    data object Completed : SyncState

    data class Failed(
        val error: Throwable,
        val retryable: Boolean
    ) : SyncState
}
