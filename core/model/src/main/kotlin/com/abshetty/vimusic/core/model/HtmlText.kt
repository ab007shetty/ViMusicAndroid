package com.abshetty.vimusic.core.model

object HtmlText {
    private val NAMED = mapOf(
        "amp" to "&",
        "lt" to "<",
        "gt" to ">",
        "quot" to "\"",
        "apos" to "'",
        "nbsp" to " ",
        "hellip" to "…",
        "mdash" to "—",
        "ndash" to "–",
        "lsquo" to "‘",
        "rsquo" to "’",
        "ldquo" to "“",
        "rdquo" to "”",
    )

    private val ENTITY = Regex("&(#x?[0-9a-fA-F]+|[a-zA-Z][a-zA-Z0-9]{1,10});")

    fun decode(text: String): String {
        if (!text.contains('&')) return text
        return ENTITY.replace(text) { match ->
            val body = match.groupValues[1]
            when {
                body.startsWith("#x") || body.startsWith("#X") ->
                    body.drop(2).toIntOrNull(16)?.toChar()?.toString() ?: match.value
                body.startsWith("#") ->
                    body.drop(1).toIntOrNull()?.toChar()?.toString() ?: match.value

                else -> NAMED[body.lowercase()] ?: match.value
            }
        }
    }
}
