package com.abshetty.vimusic.core.data.local

import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import com.abshetty.vimusic.core.datastore.SettingsStore
import com.abshetty.vimusic.core.model.CacheState
import com.abshetty.vimusic.core.model.LocalId
import com.abshetty.vimusic.core.model.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext

@Singleton
class LocalMusicRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: SettingsStore,
) {
    val folderUri: Flow<String?> = settings.localFolderUri

    @Volatile private var _folderUri: String? = null

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    private val _scanning = MutableStateFlow(false)
    val scanning: StateFlow<Boolean> = _scanning.asStateFlow()

    val folderName: StateFlow<String?> get() = _folderName
    private val _folderName = MutableStateFlow<String?>(null)

    private val generation = AtomicInteger(0)

    suspend fun setFolder(uri: Uri) {
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        }.onFailure { Log.w(TAG, "could not persist permission: " + it.message) }

        settings.setLocalFolderUri(uri.toString())
        refresh()
    }

    suspend fun clearFolder() {
        generation.incrementAndGet()
        settings.setLocalFolderUri(null)
        _songs.value = emptyList()
        _folderName.value = null
        _scanning.value = false
    }

    fun canDelete(): Boolean = runCatching {
        val stored = _folderUri ?: return false
        context.contentResolver.persistedUriPermissions.any {
            it.isWritePermission && it.uri.toString() == stored
        }
    }.getOrDefault(false)

    suspend fun delete(song: Song): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val uri = Uri.parse(LocalId.uriOf(song.id))
            val removed = DocumentsContract.deleteDocument(context.contentResolver, uri)
            if (!removed) error("The folder did not allow it")
        }.onSuccess {
            _songs.value = _songs.value.filterNot { it.id == song.id }
        }.onFailure {
            Log.w(TAG, "delete failed for " + song.title + ": " + it.message)
        }
    }

    suspend fun refresh() {
        val stored = settings.localFolderUri.first().also { _folderUri = it } ?: run {
            _songs.value = emptyList()
            _folderName.value = null
            return
        }

        val gen = generation.incrementAndGet()
        val tree = Uri.parse(stored)
        _scanning.value = true

        _folderName.value = runCatching {
            DocumentsContract.getTreeDocumentId(tree).substringAfterLast('/')
        }.getOrNull()

        try {
            val found = withContext(Dispatchers.IO) {
                runCatching { walk(tree) }
                    .onFailure { Log.w(TAG, "scan failed: " + it.message) }
                    .getOrDefault(emptyList())
            }
            if (generation.get() != gen) return

            _songs.value = found
            withContext(Dispatchers.IO) { readTags(found, gen) }
        } finally {
            if (generation.get() == gen) _scanning.value = false
        }
    }

    private suspend fun walk(tree: Uri): List<Song> {
        val rootId = DocumentsContract.getTreeDocumentId(tree)
        val pending = ArrayDeque(listOf(rootId))
        val out = mutableListOf<Song>()
        val seen = mutableSetOf<String>()

        while (pending.isNotEmpty() && out.size < MAX_FILES) {
            coroutineContext.ensureActive()
            val parentId = pending.removeFirst()
            if (!seen.add(parentId)) continue

            val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, parentId)
            val cursor = runCatching {
                context.contentResolver.query(
                    children,
                    arrayOf(
                        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                        DocumentsContract.Document.COLUMN_MIME_TYPE,
                        DocumentsContract.Document.COLUMN_LAST_MODIFIED,
                    ),
                    null, null, null,
                )
            }.getOrNull() ?: continue

            cursor.use {
                while (it.moveToNext() && out.size < MAX_FILES) {
                    val docId = it.getString(0)
                    val name = it.getString(1) ?: continue
                    val mime = it.getString(2).orEmpty()
                    val modified = if (it.isNull(3)) null else it.getLong(3)

                    if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                        pending.addLast(docId)
                        continue
                    }
                    if (!isAudioFile(mime, name)) continue

                    val fileUri = DocumentsContract.buildDocumentUriUsingTree(tree, docId)
                    out += placeholderSong(fileUri.toString(), name, modified)
                }
            }
        }
        return out.sortedBy { it.title.lowercase() }
    }

    private suspend fun readTags(found: List<Song>, gen: Int) {
        if (found.isEmpty()) return

        val enriched = found.toMutableList()
        var sinceLastPublish = 0

        for (index in found.indices) {
            coroutineContext.ensureActive()
            if (generation.get() != gen) return

            enriched[index] = tagsFor(found[index])

            if (++sinceLastPublish == PUBLISH_EVERY) {
                sinceLastPublish = 0
                _songs.value = enriched.toList()
            }
        }

        if (generation.get() == gen) _songs.value = enriched.toList()
    }

    private fun tagsFor(song: Song): Song {
        val retriever = MediaMetadataRetriever()
        return runCatching {
            retriever.setDataSource(context, Uri.parse(LocalId.uriOf(song.id)))
            song.withTags(
                title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE),
                artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST),
                durationMs = retriever
                    .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull(),
            )
        }.also {
            runCatching { retriever.release() }
        }.getOrDefault(song)
    }

    companion object {
        private const val TAG = "ViMusicLocal"

        private const val MAX_FILES = 5_000

        private const val PUBLISH_EVERY = 15
    }
}

internal fun placeholderSong(uri: String, displayName: String, modifiedMs: Long?): Song = Song(
    id = LocalId.of(uri),
    userId = LocalId.USER,

    title = displayName.substringBeforeLast('.'),

    thumbnailUrl = uri,

    likedAt = modifiedMs,

    cacheState = CacheState.CACHED,
)

internal fun Song.withTags(title: String?, artist: String?, durationMs: Long?): Song {
    val taggedTitle = title?.takeIf { it.isNotBlank() }

    val taggedArtist = artist?.takeIf { it.isNotBlank() }
    val duration = durationMs?.let(::formatDuration)

    if (taggedTitle == null && taggedArtist == null && duration == null) return this

    return copy(
        title = taggedTitle ?: this.title,
        artistsText = taggedArtist ?: artistsText,
        durationText = duration ?: durationText,
    )
}

internal fun isAudioFile(mime: String, name: String): Boolean =
    mime.startsWith("audio/") ||
        mime == "application/ogg" ||
        name.substringAfterLast('.', "").lowercase() in AUDIO_EXTENSIONS

private fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return minutes.toString() + ":" + seconds.toString().padStart(2, '0')
}

private val AUDIO_EXTENSIONS = setOf(
    "mp3", "m4a", "aac", "flac", "wav", "ogg", "opus", "wma", "mp4", "mka", "aiff", "alac",
)
