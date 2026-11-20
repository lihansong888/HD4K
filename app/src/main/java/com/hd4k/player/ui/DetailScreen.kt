package com.hd4k.player.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetailScreen(vm: MainViewModel, id: String, onBack: () -> Unit, onPlay: (Int, Int, Long) -> Unit) {
    val item = vm.item(id)
    if (item == null) { Centered("内容不存在"); return }
    val rec = vm.record(id)
    var src by remember { mutableIntStateOf((rec?.source ?: 0).coerceIn(0, (item.sources.size - 1).coerceAtLeast(0))) }
    val fav = id in vm.favorites

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip("返回") { onBack() }
            Chip(if (fav) "已收藏" else "收藏", selected = fav) { vm.toggleFavorite(id) }
        }
        Row(Modifier.padding(top = 16.dp)) {
            if (item.poster.isNotBlank()) {
                AsyncImage(
                    model = item.poster, contentDescription = item.title, contentScale = ContentScale.Crop,
                    modifier = Modifier.width(150.dp).aspectRatio(2f / 3f),
                )
            }
            Column(Modifier.weight(1f).padding(start = 16.dp)) {
                Text(item.title, style = MaterialTheme.typography.headlineSmall)
                Text(
                    listOf(item.year, item.area, item.category, item.remark).filter { it.isNotBlank() }.joinToString(" · "),
                    color = TextDim, modifier = Modifier.padding(vertical = 6.dp),
                )
                Text(item.desc, color = TextDim)
                if (item.sources.isNotEmpty()) {
                    val e = rec?.takeIf { it.source == src }?.episode ?: 0
                    Row(Modifier.padding(top = 14.dp)) {
                        Chip(if (rec != null) "继续播放" else "立即播放", selected = true) {
                            if (rec != null) onPlay(rec.source, rec.episode, rec.positionMs) else onPlay(src, 0, 0L)
                        }
                    }
                    if (e < 0) Text("")
                }
            }
        }
        if (item.sources.size > 1) {
            Text("线路", Modifier.padding(top = 18.dp, bottom = 6.dp), color = Accent)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item.sources.forEachIndexed { i, s -> Chip(s.name, selected = i == src) { src = i } }
            }
        }
        val eps = item.sources.getOrNull(src)?.episodes.orEmpty()
        if (eps.isNotEmpty()) {
            Text("选集", Modifier.padding(top = 18.dp, bottom = 6.dp), color = Accent)
            FlowRow(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                eps.forEachIndexed { i, ep -> Chip(ep.name) { onPlay(src, i, 0L) } }
            }
        }
    }
}
