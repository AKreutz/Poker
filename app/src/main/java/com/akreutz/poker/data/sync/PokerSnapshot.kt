package com.akreutz.poker.data.sync

import com.akreutz.poker.data.local.entity.PlayerEntity
import com.akreutz.poker.data.local.entity.PurgedIdEntity
import com.akreutz.poker.data.local.entity.SessionEntity
import com.akreutz.poker.data.local.entity.SessionEntryEntity

/**
 * The full shared state of the app, independent of Room. This is the shape that gets
 * serialized and pushed to / pulled from whatever [RemoteDataSource] backs it, so it must
 * stay decoupled from any particular local persistence choice.
 */
data class PokerSnapshot(
    val players: List<PlayerEntity>,
    val sessions: List<SessionEntity>,
    val entries: List<SessionEntryEntity>,
    val purgedIds: List<PurgedIdEntity> = emptyList(),
)
