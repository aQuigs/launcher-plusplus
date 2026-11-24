package com.sqftware.orbitlauncher.apps

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

// Free servers turn away some generic clients, and their usage policies ask a client to name itself.
private const val USER_AGENT = "OrbitLauncher (Android)"

/** A GET of [url] in the launcher's name, not sent until its [response] is read. */
internal fun getRequest(url: String, timeoutMillis: Int): HttpURLConnection =
    (URL(url).openConnection() as HttpURLConnection).apply {
        connectTimeout = timeoutMillis
        readTimeout = timeoutMillis
        setRequestProperty("User-Agent", USER_AGENT)
    }

/**
 * Sends the request and reads its status and, for 200, its body, which HttpURLConnection inflates from gzip on its own.
 * It blocks, so it is called off the main thread.
 */
internal fun HttpURLConnection.response(): Pair<Int, ByteArray> {
    try {
        val code = responseCode
        val body = if (code == HttpURLConnection.HTTP_OK) inputStream.use { it.readBytes() } else ByteArray(0)
        return code to body
    } finally {
        disconnect()
    }
}

/**
 * [response], given up as soon as the caller is cancelled: a blocked read ignores interruption and cancellation alike,
 * and only closing the connection under it ends it, so a search the user has typed past stops using the network.
 */
internal suspend fun HttpURLConnection.cancellableResponse(): Pair<Int, ByteArray> = suspendCancellableCoroutine { done ->
    done.invokeOnCancellation { disconnect() }
    Dispatchers.IO.asExecutor().execute { done.resumeWith(runCatching { response() }) }
}

/** The JSON at [url], got as [cancellableResponse] gets one. Throws an IOException or a JSONException if there is none. */
internal suspend fun getJson(url: String, timeoutMillis: Int): JSONObject {
    val (code, body) = getRequest(url, timeoutMillis).cancellableResponse()
    if (code != HttpURLConnection.HTTP_OK) throw IOException("HTTP $code")
    return JSONObject(body.decodeToString())
}
