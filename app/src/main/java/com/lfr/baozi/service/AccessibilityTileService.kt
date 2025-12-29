package com.example.open_autoglm_android.service

import android.content.Context
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.example.open_autoglm_android.util.AccessibilityServiceHelper

@RequiresApi(Build.VERSION_CODES.N)
class AccessibilityTileService : TileService() {

    companion object {
        private const val PREF_NAME = "keep_alive_pref"
        private const val KEY_ENABLED = "keep_alive_enabled"

        fun isKeepAliveEnabled(context: Context): Boolean {
            return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_ENABLED, false)
        }

        fun setKeepAliveEnabled(context: Context, enabled: Boolean) {
            context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_ENABLED, enabled)
                .apply()
        }
    }

    override fun onStartListening() {
        super.onStartListening()
        // 当用户下拉通知栏、系统开始渲染 Tile 时，会回调到这里
        // 利用这一时机做一次“无感保活”检查：
        // 1. 如果开启了保活但无障碍服务实例已被系统杀死，则尝试通过 WRITE_SECURE_SETTINGS 重新拉起
        // 2. 无权限时静默失败，不影响正常下拉体验
        if (isKeepAliveEnabled(this) && !AccessibilityServiceHelper.isServiceRunning()) {
            AccessibilityServiceHelper.ensureServiceEnabledViaSecureSettings(this)
        }
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        val newState = !isKeepAliveEnabled(this)
        setKeepAliveEnabled(this, newState)
        if (newState) {
            KeepAliveService.start(this)
        } else {
            KeepAliveService.stop(this)
        }
        updateTile()
    }

    private fun updateTile() {
        qsTile?.let { tile ->
            val enabled = isKeepAliveEnabled(this)
            tile.state = if (enabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            tile.label = "AutoGLM保活"
            tile.contentDescription = if (enabled) "保活已启用" else "保活已关闭"
            tile.updateTile()
        }
    }
}

