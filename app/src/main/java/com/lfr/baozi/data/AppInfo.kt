package com.lfr.baozi.data

import android.graphics.drawable.Drawable

/**
 * 应用信息数据类
 */
data class AppInfo(
    val appName: String,
    val packageName: String,
    val icon: Drawable?,
    val isSystemApp: Boolean,
    val isEnabled: Boolean = false
)

