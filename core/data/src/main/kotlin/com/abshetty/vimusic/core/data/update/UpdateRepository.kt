package com.abshetty.vimusic.core.data.update

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.utils.io.jvm.javaio.copyTo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class Update(
    val versionName: String,
    val notes: String,
    val downloadUrl: String,
    val sizeBytes: Long,
)

@Serializable
private data class GithubRelease(
    @SerialName("tag_name") val tagName: String = "",
    val name: String = "",
    val body: String = "",
    val draft: Boolean = false,
    val prerelease: Boolean = false,
    val assets: List<GithubAsset> = emptyList(),
)

@Serializable
private data class GithubAsset(
    val name: String = "",
    @SerialName("browser_download_url") val downloadUrl: String = "",
    val size: Long = 0,
)

@Singleton
class UpdateRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val client by lazy { HttpClient(OkHttp) }

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun check(currentVersionName: String): Result<Update?> = withContext(Dispatchers.IO) {
        runCatching {
            val body = client.get(LATEST_RELEASE_URL).bodyAsText()
            val release = json.decodeFromString(GithubRelease.serializer(), body)
            if (release.draft || release.prerelease) return@runCatching null

            val latest = release.tagName.removePrefix("v").trim()
            if (!isNewer(latest, currentVersionName)) return@runCatching null

            val asset = release.assets.firstOrNull { it.name.endsWith(".apk") }
                ?: return@runCatching null

            Update(
                versionName = latest,
                notes = release.body.trim().ifBlank { release.name },
                downloadUrl = asset.downloadUrl,
                sizeBytes = asset.size,
            )
        }
    }

    suspend fun downloadAndInstall(update: Update): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val target = File(context.cacheDir, "ViMusic-" + update.versionName + ".apk")
            if (target.exists()) target.delete()

            val response: HttpResponse = client.get(update.downloadUrl)
            target.outputStream().use { out -> response.bodyAsChannel().copyTo(out) }

            val uri = FileProvider.getUriForFile(
                context,
                context.packageName + ".updates",
                target,
            )

            context.startActivity(
                Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        }
    }

    private fun isNewer(candidate: String, current: String): Boolean {
        val left = candidate.split('.').map { it.toIntOrNull() ?: return false }
        val right = current.split('.').map { it.toIntOrNull() ?: return false }

        for (i in 0 until maxOf(left.size, right.size)) {
            val a = left.getOrElse(i) { 0 }
            val b = right.getOrElse(i) { 0 }
            if (a != b) return a > b
        }
        return false
    }

    private companion object {
        const val LATEST_RELEASE_URL =
            "https://api.github.com/repos/ab007shetty/ViMusicAndroid/releases/latest"
    }
}
