package com.abshetty.vimusic.core.model

object Artwork {
    const val LARGE = 1000
    const val THUMB = 240

    fun isOnDevice(url: String?): Boolean = url?.startsWith("content://") == true

    private fun upgradeYtImg(url: String): String =
        url.replace(Regex("""/(default|mqdefault)\.jpg$"""), "/hqdefault.jpg")

    fun at(url: String?, size: Int = LARGE): String? {
        if (url.isNullOrBlank()) return url

        if ("?" in url) return url

        return url

            .replace(Regex("""=w\d+-h\d+"""), "=w" + size + "-h" + size)

            .let(::upgradeYtImg)

            .replace(Regex("""/s\d+-c-k"""), "/s" + size + "-c-k")

            .replace(Regex("""/w\d+-h\d+"""), "/w" + size + "-h" + size)
    }
}
