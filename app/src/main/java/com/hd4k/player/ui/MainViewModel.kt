package com.hd4k.player.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hd4k.player.data.Catalog
import com.hd4k.player.data.Item
import com.hd4k.player.data.Record
import com.hd4k.player.data.Repository
import com.hd4k.player.data.Store
import com.hd4k.player.data.UpdateInfo
import com.hd4k.player.data.Updater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val store = Store(app)

    var catalog by mutableStateOf<Catalog?>(null); private set
    var loading by mutableStateOf(true); private set
    var error by mutableStateOf<String?>(null); private set
    var update by mutableStateOf<UpdateInfo?>(null)
    var updateMsg by mutableStateOf("")
    var favorites by mutableStateOf(store.favorites()); private set
    var history by mutableStateOf(store.history()); private set

    init { reload(); checkUpdate(silent = true) }

    fun reload() {
        loading = true; error = null
        viewModelScope.launch {
            try {
                catalog = withContext(Dispatchers.IO) { Repository.load(getApplication()) }
            } catch (e: Exception) {
                error = e.message ?: "加载失败"
            }
            loading = false
        }
    }

    fun checkUpdate(silent: Boolean) {
        viewModelScope.launch {
            try {
                val u = withContext(Dispatchers.IO) { Updater.check() }
                update = u
                if (!silent) updateMsg = if (u == null) "已是最新版本" else ""
            } catch (e: Exception) {
                if (!silent) updateMsg = "检查失败：${e.message}"
            }
        }
    }

    fun item(id: String): Item? = catalog?.items?.firstOrNull { it.id == id }

    fun toggleFavorite(id: String) {
        favorites = if (id in favorites) favorites - id else favorites + id
        store.setFavorites(favorites)
    }

    fun record(itemId: String): Record? = history.firstOrNull { it.itemId == itemId }

    fun saveProgress(itemId: String, source: Int, episode: Int, positionMs: Long) {
        val r = Record(itemId, source, episode, positionMs, System.currentTimeMillis())
        history = (listOf(r) + history.filter { it.itemId != itemId }).take(100)
        store.saveHistory(history)
    }

    fun clearHistory() { history = emptyList(); store.saveHistory(history) }
}
