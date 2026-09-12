package com.abshetty.vimusic.core.innertube.potoken

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.JavascriptInterface
import android.webkit.WebView
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class BotGuardWebView(context: Context) {
    @SuppressLint("SetJavaScriptEnabled")
    private val webView = WebView(context).apply {
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.blockNetworkLoads = false
    }

    private var ready = false

    private suspend fun ensureLoaded() {
        if (ready) return
        val loaded = CompletableDeferred<Unit>()
        webView.webViewClient = object : android.webkit.WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                if (!loaded.isCompleted) loaded.complete(Unit)
            }
        }
        webView.loadDataWithBaseURL(
            ORIGIN,
            "<!doctype html><html><head></head><body></body></html>",
            "text/html",
            "utf-8",
            null,
        )
        loaded.await()
        ready = true
    }

    private suspend fun eval(script: String): String? {
        val result = CompletableDeferred<String?>()
        webView.evaluateJavascript(script) { value ->
            result.complete(value?.takeIf { it != "null" }?.trim('"'))
        }
        return result.await()
    }

    suspend fun snapshot(interpreterJs: String, program: String): String? =
        withContext(Dispatchers.Main) {
            withTimeoutOrNull(TIMEOUT_MS) {
                ensureLoaded()

                eval(
                    """
                    (function () {
                      window.__before = new Set(Object.getOwnPropertyNames(window));
                      return 'ok';
                    })();
                    """.trimIndent()
                )

                val injected = eval(
                    """
                    (function () {
                      try {
                        var s = document.createElement('script');
                        s.textContent = ${JsString.quote(interpreterJs)};
                        document.head.appendChild(s);
                        return 'ok';
                      } catch (e) { return 'error: ' + e; }
                    })();
                    """.trimIndent()
                )
                if (injected != "ok") return@withTimeoutOrNull null

                val vm = eval(
                    """
                    (function () {
                      var added = Object.getOwnPropertyNames(window)
                        .filter(function (k) { return !window.__before.has(k); });
                      for (var i = 0; i < added.length; i++) {
                        var v = window[added[i]];
                        if (v && typeof v.a === 'function') return added[i];
                      }
                      return null;
                    })();
                    """.trimIndent()
                ) ?: return@withTimeoutOrNull null

                val response = CompletableDeferred<String?>()
                webView.addJavascriptInterface(
                    object {
                        @JavascriptInterface
                        fun onResult(value: String?) {
                            if (!response.isCompleted) response.complete(value)
                        }
                    },
                    BRIDGE,
                )

                eval(
                    """
                    (function () {
                      try {
                        window['$vm'].a(
                          ${JsString.quote(program)},
                          function (fn) {
                            fn(function (result) { $BRIDGE.onResult(result); });
                          },
                          true, undefined, function () {}
                        );
                        return 'ok';
                      } catch (e) { $BRIDGE.onResult(null); return 'error: ' + e; }
                    })();
                    """.trimIndent()
                )

                response.await()
            }
        }

    fun destroy() {
        runCatching { webView.destroy() }
    }

    private companion object {
        const val ORIGIN = "https://www.youtube.com"
        const val BRIDGE = "__vimusicBotGuard"
        const val TIMEOUT_MS = 20_000L
    }
}

internal object JsString {
    fun quote(value: String): String {
        val out = StringBuilder(value.length + 16)
        out.append('"')
        for (c in value) {
            when (c) {
                '\\' -> out.append("\\\\")
                '"' -> out.append("\\\"")
                '\n' -> out.append("\\n")
                '\r' -> out.append("\\r")
                '\t' -> out.append("\\t")

                ' ' -> out.append("\\u2028")
                ' ' -> out.append("\\u2029")
                else -> if (c < ' ') out.append("\\u%04x".format(c.code)) else out.append(c)
            }
        }
        out.append('"')
        return out.toString()
    }
}
