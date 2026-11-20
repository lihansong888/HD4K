package com.hd4k.player.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hd4k.player.data.Item

@Composable
fun SearchScreen(vm: MainViewModel, onBack: () -> Unit, onOpen: (String) -> Unit) {
    var q by remember { mutableStateOf("") }
    val all = vm.catalog?.items.orEmpty()
    val res = if (q.isBlank()) all else all.filter {
        it.title.contains(q, true) || it.category.contains(q, true) || it.area.contains(q, true) || it.year.contains(q)
    }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Chip("返回") { onBack() }
            OutlinedTextField(
                value = q, onValueChange = { q = it }, singleLine = true,
                label = { Text("输入片名搜索") },
                modifier = Modifier.weight(1f).padding(start = 12.dp),
            )
        }
        PosterGrid(res, onOpen)
    }
}

@Composable
fun PosterGrid(items: List<Item>, onOpen: (String) -> Unit) {
    LazyVerticalGrid(columns = GridCells.Adaptive(130.dp), modifier = Modifier.fillMaxSize().padding(top = 8.dp)) {
        items(items, key = { it.id }) { PosterCard(it) { onOpen(it.id) } }
    }
}

@Composable
fun ListScreen(title: String, items: List<Item>, onBack: () -> Unit, onOpen: (String) -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Chip("返回") { onBack() }
            Text(title, Modifier.padding(start = 16.dp), color = Accent)
        }
        if (items.isEmpty()) Centered("暂无内容") else PosterGrid(items, onOpen)
    }
}
