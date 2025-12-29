package com.example.open_autoglm_android.util

import android.os.Build
import java.io.File

object DeviceUtils {
    /**
     * 检测是否在模拟器上运行
     * @return true 如果在模拟器上
     */
    fun isEmulator(): Boolean {
        return (Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")
                || "google_sdk" == Build.PRODUCT
                || checkFiles())
    }
    
    /**
     * 检查模拟器特征文件
     */
    private fun checkFiles(): Boolean {
        val files = listOf(
            "/system/lib/libc_malloc_debug_qemu.so",
            "/sys/qemu_trace",
            "/system/bin/qemu-props"
        )
        return files.any { File(it).exists() }
    }
}

