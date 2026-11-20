package com.hd4k.player.data

import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

object Http {
    const val UA = "Mozilla/5.0 (Linux; Android 11) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36 HD4K/1.0"
    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    fun getString(url: String): String {
        val req = Request.Builder().url(url).header("User-Agent", UA).build()
        client.newCall(req).execute().use { r ->
            check(r.isSuccessful) { "HTTP ${r.code}" }
            return r.body?.string().orEmpty()
        }
    }
}
