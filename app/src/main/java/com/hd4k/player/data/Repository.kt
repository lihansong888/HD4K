package com.hd4k.player.data

import android.content.Context
import com.hd4k.player.BuildConfig
import kotlinx.serialization.json.Json
import java.io.File

object Repository {
    val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }

    /** 网络 → 本地缓存 → 内置示例，逐级降级 */
    fun load(ctx: Context): Catalog {
        val cache = File(ctx.filesDir, "catalog.json")
        if (BuildConfig.CATALOG_URL.isNotBlank()) {
            try {
                val text = Http.getString(BuildConfig.CATALOG_URL)
                val c = json.decodeFromString<Catalog>(text)
                cache.writeText(text)
                return c
            } catch (_: Exception) { }
            if (cache.exists()) {
                try { return json.decodeFromString(cache.readText()) } catch (_: Exception) { }
            }
        }
        if (BuildConfig.DEMO_FALLBACK) {
            val text = ctx.assets.open("sample_catalog.json").bufferedReader().use { it.readText() }
            return json.decodeFromString(text)
        }
        error("无法加载内容目录")
    }
}
