package com.akreutz.poker.data.sync

import android.util.Log
import com.akreutz.poker.data.local.dao.PlayerDao
import com.akreutz.poker.data.local.dao.SessionDao
import com.akreutz.poker.data.local.dao.SessionEntryDao
import com.akreutz.poker.data.local.entity.PlayerEntity
import com.akreutz.poker.data.local.entity.SessionEntity
import com.akreutz.poker.data.local.entity.SessionEntryEntity
import java.time.Instant

private const val TAG = "PokerSync"

/**
 * Keeps the local Room database and the remote [RemoteDataSource] in agreement.
 *
 * Merge strategy is last-write-wins per row, keyed by [PlayerEntity.id] / [SessionEntity.id] /
 * [SessionEntryEntity.id], using each row's `updatedAt`. Deletes are soft (`isDeleted = true`),
 * so a delete is just a normal row update and merges the same way as any other edit.
 *
 * This is the only place that needs to change if the merge policy ever gets smarter (e.g.
 * field-level merges); everything else only sees Room or [RemoteDataSource].
 */
class SyncManager(
    private val playerDao: PlayerDao,
    private val sessionDao: SessionDao,
    private val sessionEntryDao: SessionEntryDao,
    private val remoteDataSource: RemoteDataSource,
) {
    private var lastKnownRemoteVersion: String? = null

    /**
     * Pulls the remote snapshot (if any), merges it into Room, then pushes the merged result
     * back so the remote reflects any local-only changes too. Retries once on a version
     * conflict (i.e. another device pushed between our pull and our push).
     */
    suspend fun sync() {
        Log.d(TAG, "sync() starting")

        val remote = remoteDataSource.pull()
        if (remote == null) {
            Log.d(TAG, "pull() found no remote snapshot yet")
        } else {
            Log.d(
                TAG,
                "pull() got ${remote.snapshot.players.size} players, " +
                    "${remote.snapshot.sessions.size} sessions, " +
                    "${remote.snapshot.entries.size} entries (version=${remote.version})",
            )
        }

        val local = readLocalSnapshot()
        Log.d(
            TAG,
            "local has ${local.players.size} players, ${local.sessions.size} sessions, ${local.entries.size} entries",
        )

        val merged = if (remote == null) local else mergeSnapshots(local, remote.snapshot)

        applyToLocal(merged, currentLocal = local)
        Log.d(
            TAG,
            "merged result: ${merged.players.size} players, ${merged.sessions.size} sessions, ${merged.entries.size} entries",
        )

        val expectedVersion = remote?.version ?: lastKnownRemoteVersion
        when (val result = remoteDataSource.push(merged, expectedVersion)) {
            is PushResult.Success -> {
                lastKnownRemoteVersion = result.newVersion
                Log.d(TAG, "push() succeeded, new version=${result.newVersion}")
            }
            PushResult.Conflict -> {
                Log.w(TAG, "push() conflict, remote moved on - retrying sync")
                sync() // someone else wrote in the meantime; retry with fresh state
            }
        }
    }

    private suspend fun readLocalSnapshot(): PokerSnapshot = PokerSnapshot(
        players = playerDao.getAll(),
        sessions = sessionDao.getAll(),
        entries = sessionEntryDao.getAll(),
    )

    private suspend fun applyToLocal(merged: PokerSnapshot, currentLocal: PokerSnapshot) {
        if (merged === currentLocal) return // nothing remote to merge in, local is already correct
        playerDao.upsertAll(merged.players)
        sessionDao.upsertAll(merged.sessions)
        sessionEntryDao.upsertAll(merged.entries)
    }

    private fun mergeSnapshots(local: PokerSnapshot, remote: PokerSnapshot): PokerSnapshot = PokerSnapshot(
        players = mergeById(local.players, remote.players, PlayerEntity::id, PlayerEntity::updatedAt),
        sessions = mergeById(local.sessions, remote.sessions, SessionEntity::id, SessionEntity::updatedAt),
        entries = mergeById(local.entries, remote.entries, SessionEntryEntity::id, SessionEntryEntity::updatedAt),
    )

    private fun <T, K> mergeById(
        local: List<T>,
        remote: List<T>,
        keyOf: (T) -> K,
        updatedAtOf: (T) -> Instant,
    ): List<T> {
        val merged = local.associateBy(keyOf).toMutableMap()
        for (remoteRow in remote) {
            val key = keyOf(remoteRow)
            val localRow = merged[key]
            if (localRow == null || updatedAtOf(remoteRow) > updatedAtOf(localRow)) {
                merged[key] = remoteRow
            }
        }
        return merged.values.toList()
    }
}
