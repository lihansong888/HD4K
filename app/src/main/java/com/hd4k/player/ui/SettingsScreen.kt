package com.hd4k.player.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hd4k.player.BuildConfig

@Composable
fun SettingsScreen(vm: MainViewModel, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row { Chip("返回") { onBack() }; Text("设置", Modifier.padding(start = 16.dp), color = Accent) }
        Text("高清4K影视  v${BuildConfig.VERSION_NAME}")
        Text("内容：${vm.catalog?.items?.size ?: 0} 部", color = TextDim)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip("检查更新") { vm.checkUpdate(silent = false) }
            Chip("刷新内容") { vm.reload() }
            Chip("清空历史") { vm.clearHistory() }
        }
        if (vm.updateMsg.isNotBlank()) Text(vm.updateMsg, color = TextDim)
    }
}
