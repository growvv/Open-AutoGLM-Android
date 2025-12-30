package com.lfr.baozi

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
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.lfr.baozi.navigation.Screen
import com.lfr.baozi.ui.screen.AdvancedAuthScreen
import com.lfr.baozi.ui.screen.AppDrawerContent
import com.lfr.baozi.ui.screen.AppsScreen
import com.lfr.baozi.ui.screen.BackendSettingsScreen
import com.lfr.baozi.ui.screen.EdgeSwipeToHome
import com.lfr.baozi.ui.screen.EditProfileScreen
import com.lfr.baozi.ui.screen.InputSettingsScreen
import com.lfr.baozi.ui.screen.LoginScreen
import com.lfr.baozi.ui.screen.MainScreen
import com.lfr.baozi.ui.screen.ModelSettingsScreen
import com.lfr.baozi.ui.screen.ProfileSettingsScreen
import com.lfr.baozi.ui.screen.SettingsScreen
import com.lfr.baozi.ui.theme.OpenAutoGLMAndroidTheme
import com.lfr.baozi.ui.viewmodel.AccountViewModel
import com.lfr.baozi.ui.viewmodel.AppsViewModel
import com.lfr.baozi.ui.viewmodel.ChatViewModel
import com.lfr.baozi.ui.viewmodel.SettingsViewModel
import com.lfr.baozi.util.AccessibilityServiceHelper
import com.lfr.baozi.util.AuthHelper
import com.lfr.baozi.util.AuthHelper.hasWriteSecureSettingsPermission
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku

class MainActivity : ComponentActivity(), Shizuku.OnBinderReceivedListener,
    Shizuku.OnBinderDeadListener, ServiceConnection,
    Shizuku.OnRequestPermissionResultListener {

    companion object {
        private const val TAG = "MainActivity"
        private const val PERMISSION_CODE = 10001
    }

    private val settingsViewModel by viewModels<SettingsViewModel>()
    private val appsViewModel by viewModels<AppsViewModel>()
    private val chatViewModel by viewModels<ChatViewModel>()
    private val accountViewModel by viewModels<AccountViewModel>()

    private var accessibilityRefreshJob: Job? = null

    private var userService: IUserService? = null
    private val userServiceArgs by lazy {
        Shizuku.UserServiceArgs(ComponentName(packageName, UserService::class.java.name))
            .daemon(false)
            .processNameSuffix("adb_shell")
            .debuggable(false)
            .version(1)
    }


    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initObserver()
        initShizuku()
        enableEdgeToEdge()
        setContent {
            OpenAutoGLMAndroidTheme {
                val navController = rememberNavController()
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val scope = androidx.compose.runtime.rememberCoroutineScope()
                val chatUiState by chatViewModel.uiState.collectAsStateWithLifecycle()
                val accountUiState by accountViewModel.uiState.collectAsStateWithLifecycle()
                val currentBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = currentBackStackEntry?.destination?.route ?: Screen.Main.name

                if (!accountUiState.isLoggedIn) {
                    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        LoginScreen(viewModel = accountViewModel)
                    }
                    return@OpenAutoGLMAndroidTheme
                }

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    gesturesEnabled = currentRoute == Screen.Main.name,
                    drawerContent = {
                        AppDrawerContent(
                            conversations = chatUiState.conversations,
                            currentConversationId = chatUiState.currentConversationId,
                            userName = accountUiState.nickname,
                            avatarUri = accountUiState.avatarUri,
                            onNavigateProfileSettings = {
                                scope.launch {
                                    drawerState.close()
                                    navController.navigate(Screen.ProfileSettings.name) {
                                        launchSingleTop = true
                                    }
                                }
                            },
                            onNavigateSettings = {
                                scope.launch {
                                    drawerState.close()
                                    navController.navigate(Screen.Settings.name) {
                                        launchSingleTop = true
                                    }
                                }
                            },
                            onTaskSelected = { id, title ->
                                scope.launch {
                                    drawerState.close()
                                    chatViewModel.switchConversation(id, title)
                                }
                            },
                            onNewTask = {
                                scope.launch {
                                    drawerState.close()
                                    chatViewModel.openNewTaskDraft()
                                }
                            },
                            onRenameTask = { id, title -> chatViewModel.renameConversation(id, title) },
                            onSetPinned = { id, pinned -> chatViewModel.setConversationPinned(id, pinned) },
                            onDeleteTask = { id -> chatViewModel.deleteConversation(id) }
                        )
                    }
                ) {
                    Scaffold(
                        topBar = {
                            val showTopBar =
                                currentRoute == Screen.Main.name || currentRoute == Screen.Settings.name
                            if (showTopBar) {
                                TopAppBar(
                                    navigationIcon = {
                                        if (currentRoute == Screen.Main.name) {
                                            IconButton(
                                                modifier = Modifier.testTag("drawer_toggle"),
                                                onClick = {
                                                    scope.launch {
                                                        drawerState.open()
                                                    }
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Menu,
                                                    contentDescription = "菜单"
                                                )
                                            }
                                        } else {
                                            IconButton(
                                                onClick = {
                                                    navController.popBackStack(Screen.Main.name, false)
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                                    contentDescription = "返回首页"
                                                )
                                            }
                                        }
                                    },
                                    title = {
                                        Text(
                                            when (currentRoute) {
                                                Screen.Settings.name -> "设置"
                                                else -> chatUiState.currentConversationTitle ?: "AutoGLM"
                                            },
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                )
                            }
                        }
                    ) { innerPadding ->
                        NavHost(
                            navController = navController,
                            startDestination = Screen.Main.name,
                            // 复刻“右侧推入设置、左侧推回首页”的效果（无淡入淡出）
                            enterTransition = {
                                val targetRoute = targetState.destination.route
                                if (targetRoute == Screen.Main.name) {
                                    slideInHorizontally(
                                        animationSpec = tween(220, easing = FastOutSlowInEasing),
                                        initialOffsetX = { -it / 3 }
                                    )
                                } else {
                                    slideInHorizontally(
                                        animationSpec = tween(220, easing = FastOutSlowInEasing),
                                        initialOffsetX = { it }
                                    )
                                }
                            },
                            exitTransition = {
                                val targetRoute = targetState.destination.route
                                if (targetRoute == Screen.Main.name) {
                                    slideOutHorizontally(
                                        animationSpec = tween(220, easing = FastOutSlowInEasing),
                                        targetOffsetX = { it }
                                    )
                                } else {
                                    slideOutHorizontally(
                                        animationSpec = tween(220, easing = FastOutSlowInEasing),
                                        targetOffsetX = { -it / 3 }
                                    )
                                }
                            },
                            popEnterTransition = {
                                slideInHorizontally(
                                    animationSpec = tween(220, easing = FastOutSlowInEasing),
                                    initialOffsetX = { -it / 3 }
                                )
                            },
                            popExitTransition = {
                                slideOutHorizontally(
                                    animationSpec = tween(220, easing = FastOutSlowInEasing),
                                    targetOffsetX = { it }
                                )
                            },
                            modifier =
                                Modifier
                                    .padding(innerPadding)
                                    .consumeWindowInsets(innerPadding)
                        ) {
                            composable(Screen.Main.name) {
                                MainScreen(
                                    viewModel = chatViewModel,
                                    onNavigateToSettings = {
                                        navController.navigate(Screen.Settings.name) {
                                            launchSingleTop = true
                                        }
                                    },
                                    onNavigateToInputSettings = {
                                        navController.navigate(Screen.InputSettings.name) {
                                            launchSingleTop = true
                                        }
                                    }
                                )
                            }

                            composable(Screen.Settings.name) {
                                EdgeSwipeToHome(
                                    enabled = true,
                                    onSwipe = { navController.popBackStack(Screen.Main.name, false) }
                                ) { m ->
                                    SettingsScreen(
                                        modifier = m,
                                        viewModel = settingsViewModel,
                                        accountViewModel = accountViewModel,
                                        onNavigateToAdvancedAuth = {
                                            navController.navigate(Screen.AdvancedAuth.name)
                                        },
                                        onNavigateToAppsSettings = {
                                            navController.navigate(Screen.AppsSettings.name)
                                        },
                                        onNavigateToModelSettings = {
                                            navController.navigate(Screen.ModelSettings.name)
                                        },
                                        onNavigateToInputSettings = {
                                            navController.navigate(Screen.InputSettings.name)
                                        }
                                    )
                                }
                            }

                            composable(Screen.AppsSettings.name) {
                                EdgeSwipeToHome(
                                    enabled = true,
                                    onSwipe = { navController.popBackStack(Screen.Main.name, false) }
                                ) { m ->
                                    AppsScreen(
                                        modifier = m,
                                        viewModel = appsViewModel,
                                        onBack = { navController.popBackStack() }
                                    )
                                }
                            }

                            composable(Screen.ModelSettings.name) {
                                EdgeSwipeToHome(
                                    enabled = true,
                                    onSwipe = { navController.popBackStack(Screen.Main.name, false) }
                                ) { m ->
                                    ModelSettingsScreen(
                                        modifier = m,
                                        viewModel = settingsViewModel,
                                        onBack = { navController.popBackStack() },
                                        onNavigateToBackendSettings = {
                                            navController.navigate(Screen.BackendSettings.name)
                                        }
                                    )
                                }
                            }

                            composable(Screen.BackendSettings.name) {
                                EdgeSwipeToHome(
                                    enabled = true,
                                    onSwipe = { navController.popBackStack(Screen.Main.name, false) }
                                ) { m ->
                                    BackendSettingsScreen(
                                        modifier = m,
                                        viewModel = settingsViewModel,
                                        onBack = { navController.popBackStack() }
                                    )
                                }
                            }

                            composable(Screen.ProfileSettings.name) {
                                EdgeSwipeToHome(
                                    enabled = true,
                                    onSwipe = { navController.popBackStack(Screen.Main.name, false) }
                                ) { m ->
                                    ProfileSettingsScreen(
                                        modifier = m,
                                        viewModel = accountViewModel,
                                        onBack = { navController.popBackStack() },
                                        onNavigateEdit = {
                                            navController.navigate(Screen.ProfileEdit.name)
                                        }
                                    )
                                }
                            }

                            composable(Screen.ProfileEdit.name) {
                                EdgeSwipeToHome(
                                    enabled = true,
                                    onSwipe = { navController.popBackStack() }
                                ) { m ->
                                    EditProfileScreen(
                                        modifier = m,
                                        viewModel = accountViewModel,
                                        onCancel = { navController.popBackStack() },
                                        onDone = { navController.popBackStack() }
                                    )
                                }
                            }

                            composable(Screen.InputSettings.name) {
                                EdgeSwipeToHome(
                                    enabled = true,
                                    onSwipe = { navController.popBackStack(Screen.Main.name, false) }
                                ) { m ->
                                    InputSettingsScreen(
                                        modifier = m,
                                        viewModel = settingsViewModel,
                                        onBack = { navController.popBackStack() }
                                    )
                                }
                            }

                            composable(Screen.AdvancedAuth.name) {
                                Surface(
                                    modifier = Modifier.fillMaxSize(),
                                    color = MaterialTheme.colorScheme.background
                                ) {
                                    EdgeSwipeToHome(
                                        enabled = true,
                                        onSwipe = { navController.popBackStack(Screen.Main.name, false) }
                                    ) { m ->
                                        Box(modifier = m) {
                                            AdvancedAuthScreen(
                                                onBack = { navController.popBackStack() }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // 从系统设置页返回时，立即刷新无障碍状态，避免进入“设置”页短暂显示未启用
        accessibilityRefreshJob?.cancel()
        accessibilityRefreshJob =
            lifecycleScope.launch {
                settingsViewModel.checkAccessibilityService()
                chatViewModel.refreshAccessibilityState()
                delay(300)
                settingsViewModel.checkAccessibilityService()
                chatViewModel.refreshAccessibilityState()
                delay(1200)
                settingsViewModel.checkAccessibilityService()
                chatViewModel.refreshAccessibilityState()
            }
    }

    private fun initObserver() {
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

    private fun initShizuku() {
        // 添加权限申请监听
        Shizuku.addRequestPermissionResultListener(this)
        // Shizuku服务启动时调用该监听
        Shizuku.addBinderReceivedListenerSticky(this)
        // Shizuku服务终止时调用该监听
        Shizuku.addBinderDeadListener(this)
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

    override fun onDestroy() {
        super.onDestroy()
        // 移除权限申请监听
        Shizuku.removeRequestPermissionResultListener(this)
        Shizuku.removeBinderReceivedListener(this)
        Shizuku.removeBinderDeadListener(this)
        try {
            Shizuku.unbindUserService(userServiceArgs, this, true)
        } catch (_: IllegalStateException) {
            // Shizuku binder may not be available (e.g. during instrumentation tests)
        }
    }
}
