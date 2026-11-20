package com.hd4k.player.data

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer

@Serializable
data class Record(
    val itemId: String,
    val source: Int = 0,
    val episode: Int = 0,
    val positionMs: Long = 0,
    val updatedAt: Long = 0,
)

class Store(ctx: Context) {
    private val sp = ctx.getSharedPreferences("hd4k", Context.MODE_PRIVATE)

    fun favorites(): Set<String> = sp.getStringSet("fav", emptySet()).orEmpty().toSet()
    fun setFavorites(s: Set<String>) = sp.edit().putStringSet("fav", s).apply()

    fun history(): List<Record> = try {
        Repository.json.decodeFromString(ListSerializer(Record.serializer()), sp.getString("hist", "[]").orEmpty())
    } catch (_: Exception) { emptyList() }

    fun saveHistory(list: List<Record>) = sp.edit()
        .putString("hist", Repository.json.encodeToString(ListSerializer(Record.serializer()), list)).apply()
}
