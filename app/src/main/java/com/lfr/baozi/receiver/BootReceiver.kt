package com.example.open_autoglm_android.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.open_autoglm_android.service.AccessibilityTileService
import com.example.open_autoglm_android.service.KeepAliveService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_LOCKED_BOOT_COMPLETED) {
            // 开机后自动启动保活服务
            if (AccessibilityTileService.isKeepAliveEnabled(context)) {
                try {
                    KeepAliveService.start(context)
                } catch (e: Exception) {
                    // Android 12+ 可能无法从后台启动前台服务
                    // 依赖无障碍服务启动时自动恢复
                    Log.w("BootReceiver", "无法启动保活服务，将由无障碍服务恢复", e)
                }
            }
        }
    }
}

