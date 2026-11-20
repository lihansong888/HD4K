package com.hd4k.player.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext

sealed interface Route {
    data object Home : Route
    data class Detail(val id: String) : Route
    data object Search : Route
    data object Favorites : Route
    data object History : Route
    data object Settings : Route
    data class Play(val id: String, val source: Int, val episode: Int, val positionMs: Long) : Route
}

@Composable
fun AppRoot(vm: MainViewModel) {
    val ctx = LocalContext.current
    val stack = remember { mutableStateListOf<Route>(Route.Home) }
    val route = stack.last()
    fun go(r: Route) { stack.add(r) }
    fun back() { if (stack.size > 1) stack.removeAt(stack.lastIndex) }
    BackHandler(enabled = stack.size > 1) { back() }
    var dismissed by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(Bg)) {
        when (route) {
            Route.Home -> HomeScreen(vm, onOpen = { go(Route.Detail(it)) }, onNav = { go(it) })
            is Route.Detail -> DetailScreen(vm, route.id, onBack = ::back,
                onPlay = { s, e, p -> go(Route.Play(route.id, s, e, p)) })
            Route.Search -> SearchScreen(vm, onBack = ::back, onOpen = { go(Route.Detail(it)) })
            Route.Favorites -> ListScreen("我的收藏", vm.favorites.mapNotNull { vm.item(it) }, ::back) { go(Route.Detail(it)) }
            Route.History -> ListScreen("观看历史", vm.history.mapNotNull { vm.item(it.itemId) }, ::back) { go(Route.Detail(it)) }
            Route.Settings -> SettingsScreen(vm, onBack = ::back)
            is Route.Play -> {
                val item = vm.item(route.id)
                if (item != null) PlayerScreen(
                    item = item, srcStart = route.source, epStart = route.episode, startMs = route.positionMs,
                    onProgress = { s, e, p -> vm.saveProgress(item.id, s, e, p) },
                    onBack = ::back,
                )
            }
        }
    }

    val u = vm.update
    if (u != null && !dismissed && route !is Route.Play) {
        AlertDialog(
            onDismissRequest = { if (!u.force) dismissed = true },
            title = { Text("发现新版本 ${u.versionName}") },
            text = { Text(u.notes.ifBlank { "建议更新到最新版本" }) },
            confirmButton = {
                TextButton(onClick = {
                    ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(u.url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }) { Text("立即更新") }
            },
            dismissButton = { if (!u.force) TextButton(onClick = { dismissed = true }) { Text("稍后") } },
        )
    }
}
