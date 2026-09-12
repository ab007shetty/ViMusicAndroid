package com.abshetty.vimusic.core.data.sync

import com.abshetty.vimusic.core.database.entity.SongEntity
import kotlin.math.max

object ConflictResolver {
    fun resolve(local: SongEntity, remote: SongEntity): SongEntity {
        val localLiked = local.likedAt
        val remoteLiked = remote.likedAt

        val localStamp = local.lastPlayedAt ?: localLiked ?: 0L
        val remoteStamp = remote.lastPlayedAt ?: remoteLiked ?: 0L
        val localIsNewer = localStamp >= remoteStamp

        return local.copy(

            likedAt = when {
                localLiked != null && remoteLiked != null -> max(localLiked, remoteLiked)
                localIsNewer -> localLiked
                else -> remoteLiked
            },

            totalPlayTimeMs = max(local.totalPlayTimeMs, remote.totalPlayTimeMs),
            lastPlayedAt = maxOfNullable(local.lastPlayedAt, remote.lastPlayedAt),
            title = remote.title.ifBlank { local.title },
            artistsText = remote.artistsText ?: local.artistsText,
            durationText = remote.durationText ?: local.durationText,
            thumbnailUrl = remote.thumbnailUrl ?: local.thumbnailUrl,
            channelId = remote.channelId ?: local.channelId,

            cacheState = local.cacheState,
            downloadState = local.downloadState,
            dirty = false,
        )
    }

    private fun maxOfNullable(a: Long?, b: Long?): Long? = when {
        a == null -> b
        b == null -> a
        else -> max(a, b)
    }
}
