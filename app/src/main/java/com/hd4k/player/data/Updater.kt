package com.hd4k.player.data

import com.hd4k.player.BuildConfig

object Updater {
    fun isNewer(remote: String, local: String): Boolean {
        val a = remote.split('.').map { it.filter(Char::isDigit).toIntOrNull() ?: 0 }
        val b = local.split('.').map { it.filter(Char::isDigit).toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }; val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }

    /** 有新版本返回 UpdateInfo，否则 null；未配置更新地址时直接 null */
    fun check(): UpdateInfo? {
        if (BuildConfig.UPDATE_URL.isBlank()) return null
        val info = Repository.json.decodeFromString<UpdateInfo>(Http.getString(BuildConfig.UPDATE_URL))
        return if (info.url.isNotBlank() && isNewer(info.versionName, BuildConfig.VERSION_NAME)) info else null
    }
}
