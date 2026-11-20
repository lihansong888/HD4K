package com.hd4k.player.ui

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import com.hd4k.player.data.Http
import com.hd4k.player.data.Item
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    item: Item, srcStart: Int, epStart: Int, startMs: Long,
    onProgress: (source: Int, episode: Int, positionMs: Long) -> Unit,
    onBack: () -> Unit,
) {
    val ctx = LocalContext.current
    var srcIdx by remember { mutableIntStateOf(srcStart.coerceIn(0, (item.sources.size - 1).coerceAtLeast(0))) }
    var resumeEp by remember { mutableIntStateOf(epStart) }
    var resumeMs by remember { mutableLongStateOf(startMs) }
    var failed by remember { mutableStateOf(false) }
    var retry by remember { mutableIntStateOf(0) }

    BackHandler { onBack() }

    // 沉浸式全屏 + 保持亮屏
    DisposableEffect(Unit) {
        val w = (ctx as? Activity)?.window
        val c = w?.let { WindowCompat.getInsetsController(it, it.decorView) }
        c?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        c?.hide(WindowInsetsCompat.Type.systemBars())
        onDispose { c?.show(WindowInsetsCompat.Type.systemBars()) }
    }

    val src = item.sources.getOrNull(srcIdx)
    if (src == null || src.episodes.isEmpty()) {
        Box(Modifier.fillMaxSize().background(Color.Black), Alignment.Center) { Text("暂无可播放线路") }
        return
    }

    val player = remember(srcIdx, retry) {
        val ds = OkHttpDataSource.Factory(Http.client)
            .setUserAgent(Http.UA)
            .setDefaultRequestProperties(src.headers)
        val renderers = DefaultRenderersFactory(ctx)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
        ExoPlayer.Builder(ctx, renderers)
            .setMediaSourceFactory(DefaultMediaSourceFactory(ds))
            .build().apply {
                val mime = when (src.format.lowercase()) {
                    "hls", "m3u8" -> MimeTypes.APPLICATION_M3U8
                    "dash", "mpd" -> MimeTypes.APPLICATION_MPD
                    else -> null
                }
                val items = src.episodes.map {
                    MediaItem.Builder().setUri(it.url).apply { if (mime != null) setMimeType(mime) }.build()
                }
                setMediaItems(items, resumeEp.coerceIn(0, items.size - 1), resumeMs)
                playWhenReady = true
                prepare()
            }
    }

    DisposableEffect(player) {
        val l = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                resumeEp = player.currentMediaItemIndex
                resumeMs = player.currentPosition
                if (srcIdx + 1 < item.sources.size) {
                    Toast.makeText(ctx, "线路失败，自动切换：${item.sources[srcIdx + 1].name}", Toast.LENGTH_SHORT).show()
                    srcIdx += 1
                } else failed = true
            }
        }
        player.addListener(l)
        onDispose {
            onProgress(srcIdx, player.currentMediaItemIndex, player.currentPosition)
            player.removeListener(l)
            player.release()
        }
    }

    LaunchedEffect(player) {
        while (true) {
            delay(5000)
            onProgress(srcIdx, player.currentMediaItemIndex, player.currentPosition)
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { c ->
                PlayerView(c).apply {
                    useController = true
                    keepScreenOn = true
                    isFocusable = true
                    isFocusableInTouchMode = true
                    requestFocus()
                }
            },
            update = { it.player = player },
        )
        if (failed) {
            Column(Modifier.align(Alignment.Center).background(Color(0xCC000000)), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("所有线路均播放失败", color = Color.White)
                Chip("重试") { failed = false; srcIdx = 0; retry++ }
            }
        }
    }
}
