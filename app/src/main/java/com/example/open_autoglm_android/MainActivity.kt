package com.example.open_autoglm_android

import android.content.ComponentName
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.open_autoglm_android.navigation.Screen
import com.example.open_autoglm_android.ui.screen.AdvancedAuthScreen
import com.example.open_autoglm_android.ui.screen.AppsScreen
import com.example.open_autoglm_android.ui.screen.MainScreen
import com.example.open_autoglm_android.ui.screen.ModelConfigScreen
import com.example.open_autoglm_android.ui.screen.PromptLogScreen
import com.example.open_autoglm_android.ui.screen.SettingsScreen
import com.example.open_autoglm_android.ui.theme.OpenAutoGLMAndroidTheme
import com.example.open_autoglm_android.ui.viewmodel.AppsViewModel
import com.example.open_autoglm_android.ui.viewmodel.SettingsViewModel
import com.example.open_autoglm_android.util.AccessibilityServiceHelper
import com.example.open_autoglm_android.util.AuthHelper
import com.example.open_autoglm_android.util.AuthHelper.hasWriteSecureSettingsPermission
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku

class MainActivity : ComponentActivity(), Shizuku.OnBinderReceivedListener,
    Shizuku.OnBinderDeadListener, ServiceConnection,
    Shizuku.OnRequestPermissionResultListener, LifecycleEventObserver {

    companion object {
        private const val APPLICATION_ID = "com.example.open_autoglm_android"
        private const val TAG = "MainActivity"
        private const val PERMISSION_CODE = 10001
    }

    private val settingsViewModel by viewModels<SettingsViewModel>()
    private val appsViewModel by viewModels<AppsViewModel>()

    private var userService: IUserService? = null
    private val userServiceArgs = Shizuku.UserServiceArgs(ComponentName(APPLICATION_ID, UserService::class.java.name))
            .daemon(false)
            .processNameSuffix("adb_shell")
            .debuggable(false)
            .version(1)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initObserver()
        enableEdgeToEdge()
        setContent {
            OpenAutoGLMAndroidTheme {
                var selectedTab by rememberSaveable { mutableStateOf(Screen.Main) }
                val navController = rememberNavController()

                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                icon = { Icon(Icons.Default.Home, contentDescription = "首页") },
                                label = { Text("首页") },
                                selected = selectedTab == Screen.Main,
                                onClick = {
                                    selectedTab = Screen.Main
                                    if (navController.currentDestination?.route != Screen.Main.name) {
                                        navController.navigate(Screen.Main.name) {
                                            popUpTo(Screen.Main.name) { inclusive = false }
                                            launchSingleTop = true
                                        }
                                    }
                                }
                            )
                            NavigationBarItem(
                                icon = { Icon(Icons.Default.Apps, contentDescription = "应用") },
                                label = { Text("应用") },
                                selected = selectedTab == Screen.Apps,
                                onClick = {
                                    selectedTab = Screen.Apps
                                    if (navController.currentDestination?.route != Screen.Apps.name) {
                                        navController.navigate(Screen.Apps.name) {
                                            popUpTo(Screen.Main.name) { inclusive = false }
                                            launchSingleTop = true
                                        }
                                    }
                                }
                            )
                            NavigationBarItem(
                                icon = {
                                    Icon(
                                        Icons.Default.Description,
                                        contentDescription = "日志"
                                    )
                                },
                                label = { Text("日志") },
                                selected = selectedTab == Screen.PromptLog,
                                onClick = {
                                    selectedTab = Screen.PromptLog
                                    if (navController.currentDestination?.route != Screen.PromptLog.name) {
                                        navController.navigate(Screen.PromptLog.name) {
                                            popUpTo(Screen.Main.name) { inclusive = false }
                                            launchSingleTop = true
                                        }
                                    }
                                }
                            )
                            NavigationBarItem(
                                icon = {
                                    Icon(
                                        Icons.Default.Settings,
                                        contentDescription = "设置"
                                    )
                                },
                                label = { Text("设置") },
                                selected = selectedTab == Screen.Settings,
                                onClick = {
                                    selectedTab = Screen.Settings
                                    if (navController.currentDestination?.route != Screen.Settings.name) {
                                        navController.navigate(Screen.Settings.name) {
                                            popUpTo(Screen.Main.name) { inclusive = false }
                                            launchSingleTop = true
                                        }
                                    }
                                }
                            )
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = Screen.Main.name,
                        modifier = Modifier
                            .padding(innerPadding)
                            .consumeWindowInsets(innerPadding)
                    ) {
                        composable(Screen.Main.name) {
                            MainScreen()
                        }
                        composable(Screen.PromptLog.name) {
                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                color = MaterialTheme.colorScheme.background
                            ) {
                                PromptLogScreen()
                            }
                        }
                        composable(Screen.Apps.name) {
                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                color = MaterialTheme.colorScheme.background
                            ) {
                                AppsScreen(viewModel = appsViewModel)
                            }
                        }
                        composable(Screen.AdvancedAuth.name) {
                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                color = MaterialTheme.colorScheme.background
                            ) {
                                AdvancedAuthScreen(
                                    onBack = { navController.popBackStack() }
                                )
                            }
                        }
                        composable(Screen.Settings.name) {
                            SettingsScreen(
                                viewModel = settingsViewModel,
                                onNavigateToAdvancedAuth = {
                                    navController.navigate(Screen.AdvancedAuth.name)
                                },
                                onNavigateToModelConfig = {
                                    navController.navigate(Screen.ModelConfig.name)
                                }
                            )
                        }
                        composable(Screen.ModelConfig.name){
                            ModelConfigScreen {
                                navController.popBackStack()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun initObserver() {
        lifecycle.addObserver(this)
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                settingsViewModel.uiState.collect { uiState ->
                    if (uiState.floatingWindowEnabled) {
                        settingsViewModel.setFloatingWindowEnabled(true)
                    }
                    if (!uiState.isAccessibilityEnabled) {
                        if (hasWriteSecureSettingsPermission(this@MainActivity)) {
                            AccessibilityServiceHelper.ensureServiceEnabledViaSecureSettings(this@MainActivity)
                        }
                    }
                }
            }
        }
    }

    override fun onBinderReceived() {
        Log.i(TAG, "Shizuku 服务已启动")
        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            connectShizuku()
        } else {
            AuthHelper.requestShizukuPermission(this@MainActivity, PERMISSION_CODE)
        }
    }

    override fun onBinderDead() {
        Log.i(TAG, "Shizuku 服务已终止")
    }

    override fun onServiceConnected(p0: ComponentName?, binder: IBinder?) {
        Log.i(TAG, "Shizuku 服务服务已连接")
        if (binder != null && binder.pingBinder()) {
            userService = IUserService.Stub.asInterface(binder)
            if (hasWriteSecureSettingsPermission(this@MainActivity)) {
                return
            }

            val packageName = packageName
            val permission = "android.permission.WRITE_SECURE_SETTINGS"
            val command = "pm grant $packageName $permission"
            val code = userService?.execArr(arrayOf("sh", "-c", command))
            lifecycleScope.launch {
                if (code != -1) {
                    delay(500)
                    if (hasWriteSecureSettingsPermission(this@MainActivity)) {
                        Toast.makeText(this@MainActivity, "无感保活已开启", Toast.LENGTH_SHORT)
                            .show()
                        AccessibilityServiceHelper.ensureServiceEnabledViaSecureSettings(this@MainActivity)
                    }
                }
            }
        }
    }

    override fun onServiceDisconnected(p0: ComponentName?) {
        Log.i(TAG, "Shizuku 服务服务已断开")
    }

    override fun onRequestPermissionResult(requestCode: Int, grantResult: Int) {
        if (grantResult == PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "Shizuku 授权成功")
            connectShizuku()
        } else {
            Log.i(TAG, "Shizuku 授权失败")
        }
    }

    private fun connectShizuku() {
        if (userService != null) {
            Log.i(TAG, "已连接Shizuku服务")
            return
        }

        Shizuku.bindUserService(userServiceArgs, this)
    }

    override fun onStateChanged(
        source: LifecycleOwner,
        event: Lifecycle.Event
    ) {
        when(event){
            Lifecycle.Event.ON_START -> {
                Shizuku.addRequestPermissionResultListener(this)
                Shizuku.addBinderReceivedListenerSticky(this)
                Shizuku.addBinderDeadListener(this)
            }

            Lifecycle.Event.ON_STOP -> {
                Shizuku.removeRequestPermissionResultListener(this)
                Shizuku.removeBinderReceivedListener(this)
                Shizuku.removeBinderDeadListener(this)
            }

            Lifecycle.Event.ON_DESTROY -> {
                if (userService != null && Shizuku.pingBinder()){
                    Shizuku.unbindUserService(userServiceArgs,this,false)
                }
            }

            else -> {}
        }
    }
}
