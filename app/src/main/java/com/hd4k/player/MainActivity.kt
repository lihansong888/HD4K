package com.hd4k.player

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.hd4k.player.ui.AppRoot
import com.hd4k.player.ui.HdTheme
import com.hd4k.player.ui.MainViewModel

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { HdTheme { AppRoot(vm) } }
    }
}
