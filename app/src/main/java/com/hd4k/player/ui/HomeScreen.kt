package com.hd4k.player.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.hd4k.player.data.Item
import com.hd4k.player.util.isTelevision

@Composable
fun HomeScreen(vm: MainViewModel, onOpen: (String) -> Unit, onNav: (Route) -> Unit) {
    val appCtx = LocalContext.current
    val tv = remember { isTelevision(appCtx) }
    val catalog = vm.catalog
    var cat by remember { mutableStateOf("全部") }
    val pad = if (tv) 32.dp else 12.dp

    Column(Modifier.fillMaxSize().padding(horizontal = pad, vertical = 12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(catalog?.name ?: "高清4K影视", style = MaterialTheme.typography.titleLarge, color = Accent)
            Box(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("搜索") { onNav(Route.Search) }
                Chip("历史") { onNav(Route.History) }
                Chip("收藏") { onNav(Route.Favorites) }
                Chip("设置") { onNav(Route.Settings) }
            }
        }
        when {
            vm.loading && catalog == null -> Centered("加载中…")
            catalog == null -> Column(Modifier.fillMaxSize(), Arrangement.Center, Alignment.CenterHorizontally) {
                Text(vm.error ?: "加载失败", color = TextDim)
                Chip("重试") { vm.reload() }
            }
            else -> {
                val cats = listOf("全部") + catalog.categories
                LazyRow(Modifier.padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(cats) { c -> Chip(c, selected = c == cat) { cat = c } }
                }
                val shown: List<Item> = if (cat == "全部") catalog.items else catalog.items.filter { it.category == cat }
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(if (tv) 160.dp else 110.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(shown, key = { it.id }) { PosterCard(it) { onOpen(it.id) } }
                }
            }
        }
    }
}

@Composable
fun Centered(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(text, color = TextDim) }
}
