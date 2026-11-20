package com.hd4k.player.data

import kotlinx.serialization.Serializable

@Serializable
data class Catalog(
    val name: String = "高清4K影视",
    val categories: List<String> = emptyList(),
    val items: List<Item> = emptyList(),
)

@Serializable
data class Item(
    val id: String,
    val title: String,
    val poster: String = "",
    val category: String = "",
    val year: String = "",
    val area: String = "",
    val remark: String = "",
    val desc: String = "",
    val sources: List<Source> = emptyList(),
)

/** 一条线路：一组剧集，format 可选 hls / dash / mp4 / 空(自动识别) */
@Serializable
data class Source(
    val name: String = "线路",
    val format: String = "",
    val headers: Map<String, String> = emptyMap(),
    val episodes: List<Episode> = emptyList(),
)

@Serializable
data class Episode(val name: String, val url: String)

@Serializable
data class UpdateInfo(
    val versionName: String = "",
    val url: String = "",
    val notes: String = "",
    val force: Boolean = false,
)
