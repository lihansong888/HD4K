package com.hd4k.player.util

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import android.content.pm.PackageManager

/** 电视 / 盒子 / 无触摸屏设备视为遥控器模式 */
fun isTelevision(ctx: Context): Boolean {
    val ui = ctx.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
    if (ui?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION) return true
    val pm = ctx.packageManager
    return pm.hasSystemFeature(PackageManager.FEATURE_LEANBACK) ||
        !pm.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN)
}
