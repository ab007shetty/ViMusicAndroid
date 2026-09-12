package com.abshetty.vimusic.core.innertube

import kotlinx.serialization.Serializable

@Serializable
data class ClientContext(
    val clientName: String,
    val clientVersion: String,
    val androidSdkVersion: Int? = null,
    val deviceMake: String? = null,
    val deviceModel: String? = null,
    val osName: String? = null,
    val osVersion: String? = null,
    val userAgent: String,
    val hl: String = "en",
    val gl: String = "US",
) {
    companion object {
        val ANDROID_VR = ClientContext(
            clientName = "ANDROID_VR",
            clientVersion = "1.62.27",
            androidSdkVersion = 32,
            deviceMake = "Oculus",
            deviceModel = "Quest 3",
            osName = "Android",
            osVersion = "12L",
            userAgent = "com.google.android.apps.youtube.vr.oculus/1.62.27 " +
                "(Linux; U; Android 12L; eureka-user Build/SQ3A.220605.009.A1) gzip",
        )
        val ANDROID_MUSIC = ClientContext(
            clientName = "ANDROID_MUSIC",
            clientVersion = "6.42.52",
            androidSdkVersion = 33,
            osName = "Android",
            osVersion = "13",
            userAgent = "com.google.android.apps.youtube.music/6.42.52 (Linux; U; Android 13) gzip",
        )
        val IOS = ClientContext(
            clientName = "IOS",
            clientVersion = "20.10.4",
            deviceMake = "Apple",
            deviceModel = "iPhone16,2",
            osName = "iPhone",
            osVersion = "18.3.2.22D82",
            userAgent = "com.google.ios.youtube/20.10.4 " +
                "(iPhone16,2; U; CPU iOS 18_3_2 like Mac OS X)",
        )
        val TVHTML5 = ClientContext(
            clientName = "TVHTML5_SIMPLY_EMBEDDED_PLAYER",
            clientVersion = "2.0",
            userAgent = "Mozilla/5.0 (PlayStation; PlayStation 4/12.00) AppleWebKit/605.1.15",
        )

        val WEB_REMIX = ClientContext(
            clientName = "WEB_REMIX",
            clientVersion = "1.20241202.01.00",
            userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36",
        )

        val FALLBACK_ORDER = listOf(IOS, ANDROID_VR, ANDROID_MUSIC, TVHTML5)
    }
}
