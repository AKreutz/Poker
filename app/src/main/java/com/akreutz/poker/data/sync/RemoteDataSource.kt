package com.akreutz.poker.data.sync

/**
 * Abstraction over wherever the shared source of truth actually lives (Google Drive file,
 * Firestore, a self-hosted backend, ...). Nothing outside this package should know or care
 * which one is in use - swapping backends means writing a new implementation of this
 * interface only.
 *
 * Concurrency is handled with optimistic versioning: callers must pass the [remoteVersion]
 * they last observed when pushing, and a mismatch means someone else wrote in the meantime.
 */
interface RemoteDataSource {
    /** Opaque token identifying the currently stored revision (e.g. a Drive file's `version`/etag). */
    suspend fun currentVersion(): String?

    /** Fetches the current snapshot and the version it was read at, or null if nothing has been pushed yet. */
    suspend fun pull(): VersionedSnapshot?

    /**
     * Writes [snapshot] as the new state, only if the remote is still at [expectedVersion].
     * Returns the new version on success, or [PushResult.Conflict] if the remote moved on.
     */
    suspend fun push(snapshot: PokerSnapshot, expectedVersion: String?): PushResult
}

data class VersionedSnapshot(
    val snapshot: PokerSnapshot,
    val version: String,
)

sealed interface PushResult {
    data class Success(val newVersion: String) : PushResult
    data object Conflict : PushResult
}
