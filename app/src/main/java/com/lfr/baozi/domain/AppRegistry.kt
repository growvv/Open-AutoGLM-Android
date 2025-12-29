package com.example.open_autoglm_android.domain

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.example.open_autoglm_android.data.PreferencesRepository
import kotlinx.coroutines.runBlocking

/**
 * 负责应用名称到包名的映射管理
 */
object AppRegistry {
    private const val TAG = "AppRegistry"

    // 应用名称 -> 包名 的映射
    @Volatile
    private var appPackageMap: MutableMap<String, String> = mutableMapOf()

    // 包名 -> 应用名称 的反向映射
    @Volatile
    private var packageAppMap: MutableMap<String, String> = mutableMapOf()
    
    // PreferencesRepository 用于检查应用启用状态
    @SuppressLint("StaticFieldLeak")
    private var preferencesRepository: PreferencesRepository? = null

    /**
     * 初始化 AppRegistry
     * 从设备已安装应用列表中动态构建映射
     */
    fun initialize(context: Context) {
        try {
            // 初始化 PreferencesRepository
            if (preferencesRepository == null) {
                preferencesRepository = PreferencesRepository(context.applicationContext)
            }
            
            val startTime = System.currentTimeMillis()
            val packageManager = context.packageManager
            val installedApps =
                packageManager.getInstalledApplications(PackageManager.GET_META_DATA)

            val tempAppPackageMap = mutableMapOf<String, String>()
            val tempPackageAppMap = mutableMapOf<String, String>()

            installedApps.forEach { appInfo ->
                val packageName = appInfo.packageName
                val appName = appInfo.loadLabel(packageManager).toString()

                // 建立应用名称到包名的映射（支持原始名称和小写名称）
                tempAppPackageMap[appName] = packageName
                tempAppPackageMap[appName.lowercase()] = packageName

                // 建立包名到应用名称的反向映射
                tempPackageAppMap[packageName] = appName
            }

            appPackageMap = tempAppPackageMap
            packageAppMap = tempPackageAppMap

            val duration = System.currentTimeMillis() - startTime
            Log.i(TAG, "Loaded ${installedApps.size} installed apps in ${duration}ms")
            Log.i(TAG, "Created ${appPackageMap.size} app name mappings")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to load installed apps", e)
        }
    }

    /**
     * 根据应用名称获取包名
     */
    fun getPackageName(appName: String): String {
        val trimmedName = appName.trim()
        appPackageMap[trimmedName]?.let { return it }
        appPackageMap[trimmedName.lowercase()]?.let { return it }
        appPackageMap.entries.firstOrNull { (name, _) ->
            name.contains(trimmedName, ignoreCase = true) ||
                    trimmedName.contains(name, ignoreCase = true)
        }?.let { return it.value }

        return trimmedName
    }

    /**
     * 根据包名获取应用名称
     */
    fun getAppName(packageName: String): String {
        return packageAppMap[packageName] ?: packageName
    }

    /**
     * 获取所有已安装应用的数量
     */
    fun getAppCount(): Int {
        return packageAppMap.size
    }

    /**
     * 检查应用是否已安装
     */
    fun isAppInstalled(packageName: String): Boolean {
        return packageAppMap.containsKey(packageName)
    }
    
    /**
     * 检查应用是否已启用
     */
    fun isAppEnabled(packageName: String): Boolean {
        return try {
            if (preferencesRepository == null) {
                Log.w(TAG, "PreferencesRepository not initialized, denying access to: $packageName")
                return false
            }
            runBlocking {
                preferencesRepository?.isAppEnabled(packageName) ?: false
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to check if app is enabled: $packageName", e)
            false
        }
    }
    
    /**
     * 根据应用名称获取包名
     */
    fun getPackageNameIfEnabled(appName: String): String? {
        val packageName = getPackageName(appName)
        return if (isAppEnabled(packageName)) packageName else null
    }
}
