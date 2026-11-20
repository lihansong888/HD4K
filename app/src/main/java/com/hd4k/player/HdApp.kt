package com.hd4k.player

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.hd4k.player.data.Http

class HdApp : Application(), ImageLoaderFactory {
    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .okHttpClient { Http.client }
        .crossfade(true)
        .memoryCache { MemoryCache.Builder(this).maxSizePercent(0.20).build() }
        .diskCache { DiskCache.Builder().directory(cacheDir.resolve("img")).maxSizeBytes(150L shl 20).build() }
        .build()
}
