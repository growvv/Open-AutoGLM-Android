package com.example.open_autoglm_android.ui.viewmodel

import android.app.Application
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.open_autoglm_android.data.AppInfo
import com.example.open_autoglm_android.data.PreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * AppsViewModel - 管理已安装应用列表
 */
class AppsViewModel(application: Application) : AndroidViewModel(application) {
    
    private val preferencesRepository = PreferencesRepository(application)
    
    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    val apps: StateFlow<List<AppInfo>> = _apps.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _showSystemApps = MutableStateFlow(false)
    val showSystemApps: StateFlow<Boolean> = _showSystemApps.asStateFlow()
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    private var allApps: List<AppInfo> = emptyList()
    private var enabledApps: Set<String> = emptySet()
    
    init {
        loadInstalledApps()
    }
    
    /**
     * 加载已安装的应用列表
     */
    fun loadInstalledApps() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // 先加载已启用的应用列表
                enabledApps = preferencesRepository.getEnabledAppsSync()
                
                val packageManager = getApplication<Application>().packageManager
                val installedAppsRaw = withContext(Dispatchers.IO) {
                    packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
                        .map { appInfo ->
                            val packageName = appInfo.packageName
                            AppInfo(
                                appName = appInfo.loadLabel(packageManager).toString(),
                                packageName = packageName,
                                icon = try {
                                    appInfo.loadIcon(packageManager)
                                } catch (e: Exception) {
                                    null
                                },
                                isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                                isEnabled = enabledApps.contains(packageName)
                            )
                        }
                        .sortedBy { it.appName.lowercase() }
                }

                // 首次使用默认全部授权（开启）
                val installedApps =
                    if (enabledApps.isEmpty() && installedAppsRaw.isNotEmpty()) {
                        enabledApps = installedAppsRaw.map { it.packageName }.toSet()
                        preferencesRepository.saveEnabledApps(enabledApps)
                        installedAppsRaw.map { it.copy(isEnabled = true) }
                    } else {
                        installedAppsRaw
                    }
                allApps = installedApps
                filterApps()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 切换是否显示系统应用
     */
    fun toggleShowSystemApps() {
        _showSystemApps.value = !_showSystemApps.value
        filterApps()
    }
    
    /**
     * 更新搜索查询
     */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        filterApps()
    }
    
    /**
     * 切换应用的启用状态
     */
    fun toggleAppEnabled(packageName: String, enabled: Boolean) {
        viewModelScope.launch {
            try {
                preferencesRepository.toggleAppEnabled(packageName, enabled)
                
                // 更新本地状态
                if (enabled) {
                    enabledApps = enabledApps + packageName
                } else {
                    enabledApps = enabledApps - packageName
                }
                
                // 更新应用列表
                allApps = allApps.map { app ->
                    if (app.packageName == packageName) {
                        app.copy(isEnabled = enabled)
                    } else {
                        app
                    }
                }
                filterApps()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * 选择全部应用
     */
    fun selectAllApps() {
        viewModelScope.launch {
            try {
                // 获取当前显示的应用列表
                val currentApps = _apps.value
                
                // 启用所有当前显示的应用
                currentApps.forEach { app ->
                    if (!app.isEnabled) {
                        preferencesRepository.toggleAppEnabled(app.packageName, true)
                        enabledApps = enabledApps + app.packageName
                    }
                }
                
                // 更新应用列表
                allApps = allApps.map { app ->
                    if (currentApps.any { it.packageName == app.packageName }) {
                        app.copy(isEnabled = true)
                    } else {
                        app
                    }
                }
                filterApps()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * 取消选择全部应用
     */
    fun deselectAllApps() {
        viewModelScope.launch {
            try {
                // 获取当前显示的应用列表
                val currentApps = _apps.value
                
                // 禁用所有当前显示的应用
                currentApps.forEach { app ->
                    if (app.isEnabled) {
                        preferencesRepository.toggleAppEnabled(app.packageName, false)
                        enabledApps = enabledApps - app.packageName
                    }
                }
                
                // 更新应用列表
                allApps = allApps.map { app ->
                    if (currentApps.any { it.packageName == app.packageName }) {
                        app.copy(isEnabled = false)
                    } else {
                        app
                    }
                }
                filterApps()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * 反向选择应用
     */
    fun invertSelection() {
        viewModelScope.launch {
            try {
                // 获取当前显示的应用列表
                val currentApps = _apps.value
                
                // 反转所有当前显示的应用的启用状态
                currentApps.forEach { app ->
                    val newEnabled = !app.isEnabled
                    preferencesRepository.toggleAppEnabled(app.packageName, newEnabled)
                    if (newEnabled) {
                        enabledApps = enabledApps + app.packageName
                    } else {
                        enabledApps = enabledApps - app.packageName
                    }
                }
                
                // 更新应用列表
                allApps = allApps.map { app ->
                    if (currentApps.any { it.packageName == app.packageName }) {
                        app.copy(isEnabled = !app.isEnabled)
                    } else {
                        app
                    }
                }
                filterApps()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * 过滤应用列表
     */
    private fun filterApps() {
        val query = _searchQuery.value
        val showSystem = _showSystemApps.value
        
        _apps.value = allApps
            .filter { app ->
                // 过滤系统应用
                if (!showSystem && app.isSystemApp) {
                    return@filter false
                }
                // 过滤搜索查询
                if (query.isNotBlank()) {
                    app.appName.contains(query, ignoreCase = true) ||
                    app.packageName.contains(query, ignoreCase = true)
                } else {
                    true
                }
            }
            .sortedByDescending { it.isEnabled }
    }
}
