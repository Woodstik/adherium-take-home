package com.adherium.takehome.data.model

sealed class ActuationEventRepoException(
    cause: Throwable? = null
) : RuntimeException(cause) {
    class BondingException(cause: Throwable? = null) : ActuationEventRepoException(cause)
    class ConnectionException(cause: Throwable? = null) : ActuationEventRepoException(cause)
    class ReadException(cause: Throwable? = null) : ActuationEventRepoException(cause)
    class AcknowledgeException(cause: Throwable? = null) : ActuationEventRepoException(cause)
    class PersistenceException(cause: Throwable? = null) : ActuationEventRepoException(cause)
}
