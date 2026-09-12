package com.abshetty.vimusic.core.media.audio

enum class EqPreset(val label: String, val gains: FloatArray) {
    FLAT("Flat", floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)),
    ACOUSTIC("Acoustic", floatArrayOf(5f, 5f, 4f, 1f, 2f, 2f, 3f, 4f, 3f, 2f)),
    BASS_BOOSTER("Bass booster", floatArrayOf(7f, 6f, 5f, 3f, 1f, 0f, 0f, 0f, 0f, 0f)),
    BASS_REDUCER("Bass reducer", floatArrayOf(-7f, -6f, -5f, -3f, -1f, 0f, 0f, 0f, 0f, 0f)),
    CLASSICAL("Classical", floatArrayOf(5f, 4f, 3f, 2f, -1f, -1f, 0f, 2f, 3f, 4f)),
    DANCE("Dance", floatArrayOf(6f, 5f, 2f, 0f, 1f, 3f, 5f, 4f, 3f, 0f)),
    DEEP("Deep", floatArrayOf(6f, 5f, 3f, 1f, 3f, 2f, 0f, -3f, -4f, -5f)),
    ELECTRONIC("Electronic", floatArrayOf(5f, 4f, 1f, 0f, -2f, 2f, 1f, 1f, 4f, 5f)),
    HIP_HOP("Hip-Hop", floatArrayOf(6f, 5f, 2f, 3f, -1f, -1f, 1f, 0f, 2f, 3f)),
    JAZZ("Jazz", floatArrayOf(4f, 3f, 1f, 2f, -2f, -2f, 0f, 1f, 3f, 4f)),
    LATIN("Latin", floatArrayOf(5f, 4f, 0f, 0f, -2f, -2f, -2f, 0f, 3f, 5f)),
    LOUDNESS("Loudness", floatArrayOf(7f, 6f, 0f, 0f, -2f, 0f, -1f, -5f, 5f, 1f)),
    LOUNGE("Lounge", floatArrayOf(-3f, -1f, 0f, 1f, 4f, 3f, 0f, -1f, 2f, 1f)),
    PIANO("Piano", floatArrayOf(3f, 2f, 0f, 2f, 3f, 1f, 3f, 4f, 3f, 2f)),
    POP("Pop", floatArrayOf(-2f, -1f, 0f, 2f, 4f, 4f, 2f, 0f, -1f, -2f)),
    RNB("R&B", floatArrayOf(6f, 5f, 4f, 1f, -2f, -1f, 2f, 3f, 3f, 4f)),
    ROCK("Rock", floatArrayOf(6f, 5f, 4f, 2f, -1f, -1f, 1f, 3f, 4f, 5f)),
    SMALL_SPEAKERS("Small speakers", floatArrayOf(6f, 5f, 4f, 3f, 1f, 0f, -1f, -2f, -3f, -4f)),
    SPOKEN_WORD("Spoken word", floatArrayOf(-3f, -1f, 0f, 1f, 4f, 5f, 5f, 4f, 2f, 0f)),
    TREBLE_BOOSTER("Treble booster", floatArrayOf(0f, 0f, 0f, 0f, 0f, 1f, 3f, 5f, 6f, 7f)),
    TREBLE_REDUCER("Treble reducer", floatArrayOf(0f, 0f, 0f, 0f, 0f, -1f, -3f, -5f, -6f, -7f)),
    VOCAL_BOOSTER("Vocal booster", floatArrayOf(-2f, -3f, -3f, 1f, 4f, 4f, 3f, 1f, 0f, -2f));

    companion object {
        val labels: List<String> get() = entries.map { it.label }

        fun matching(gains: FloatArray): EqPreset? =
            entries.firstOrNull { it.gains.contentEquals(gains) }
    }
}
