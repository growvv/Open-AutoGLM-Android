package com.example.open_autoglm_android.util

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import com.example.open_autoglm_android.service.AutoGLMAccessibilityService

object AccessibilityServiceHelper {
    
    /**
     * 检查无障碍服务是否已启用（通过系统设置检查）
     */
    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
            ?: return false

        // Prefer AccessibilityManager's enabled list (more reliable across OEM ROMs)
        val expectedComponentName =
            ComponentName(context.packageName, AutoGLMAccessibilityService::class.java.name)

        val enabledServices =
            accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)

        val enabledByManager =
            enabledServices.any { serviceInfo ->
                val service = serviceInfo.resolveInfo.serviceInfo
                val servicePackageName = service.packageName
                val serviceClassName =
                    if (service.name.startsWith(".")) "$servicePackageName${service.name}" else service.name

                servicePackageName == expectedComponentName.packageName &&
                    serviceClassName == expectedComponentName.className
            }

        if (enabledByManager) return true

        // Fallback: parse secure settings list
        val enabledServicesSetting =
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
                ?: return false

        val flattened = expectedComponentName.flattenToString()
        return enabledServicesSetting.contains(flattened)
    }
    
    /**
     * 检查无障碍服务是否正在运行（通过实例检查）
     */
    fun isServiceRunning(): Boolean {
        return AutoGLMAccessibilityService.isServiceEnabled()
    }

    /**
     * 通过 WRITE_SECURE_SETTINGS 将本应用的无障碍服务写入系统设置，从而实现“无感”重启。
     *
     * 前置条件：
     * - 应用具有 WRITE_SECURE_SETTINGS 权限（通常通过 adb 授权：adb shell pm grant <pkg> android.permission.WRITE_SECURE_SETTINGS）
     *
     * 逻辑：
     * - 读取当前 ENABLED_ACCESSIBILITY_SERVICES 列表
     * - 若未包含本服务，则追加写回
     * - 确保 ACCESSIBILITY_ENABLED = 1
     *
     * 注意：该方法不会弹界面，调用时机建议放在下拉通知栏渲染 Tile 等“用户可感知操作”的生命周期里。
     */
    fun ensureServiceEnabledViaSecureSettings(context: Context) {
        try {
            val cr = context.contentResolver

            val componentName = ComponentName(
                context.packageName,
                AutoGLMAccessibilityService::class.java.name
            ).flattenToString()

            val enabledServices = Settings.Secure.getString(
                cr,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: ""

            val newValue = if (enabledServices.isEmpty()) {
                componentName
            } else if (!enabledServices.contains(componentName)) {
                "$enabledServices:$componentName"
            } else {
                // 已经在列表中，无需重复写入
                enabledServices
            }

            if (newValue != enabledServices) {
                Settings.Secure.putString(
                    cr,
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                    newValue
                )
            }

            // 确保总开关为开启状态
            Settings.Secure.putInt(
                cr,
                Settings.Secure.ACCESSIBILITY_ENABLED,
                1
            )
        } catch (_: Throwable) {
            // 没有权限或 ROM 限制时静默失败，避免崩溃
        }
    }
    
    /**
     * 获取无障碍服务信息
     */
    fun getServiceInfo(context: Context): AccessibilityServiceInfo? {
        val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
            ?: return null
        
        val enabledServices = accessibilityManager.getEnabledAccessibilityServiceList(
            AccessibilityServiceInfo.FEEDBACK_ALL_MASK
        )
        
        val serviceName = AutoGLMAccessibilityService::class.java.name
        
        return enabledServices.firstOrNull { serviceInfo ->
            serviceInfo.resolveInfo.serviceInfo.name == serviceName
        }
    }
}
