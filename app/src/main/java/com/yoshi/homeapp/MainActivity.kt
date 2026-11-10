package com.yoshi.homeapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels()
    private lateinit var widgets: WidgetController

    /** ホームボタンが押されるたびに増える。ホーム画面へ戻す合図に使う。 */
    private var homePressed by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        widgets = WidgetController(this, viewModel.prefs)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                LauncherRoot(viewModel, widgets, homePressed)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        widgets.startListening()
    }

    override fun onStop() {
        super.onStop()
        widgets.stopListening()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.hasCategory(Intent.CATEGORY_HOME)) homePressed++
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }

    @Deprecated("ウィジェットの設定画面は startActivityForResult 経由でしか結果を返さない")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (!widgets.onActivityResult(requestCode, resultCode)) {
            @Suppress("DEPRECATION")
            super.onActivityResult(requestCode, resultCode, data)
        }
    }
}

/** 左スワイプ（指を右から左へ）でアプリ一覧を開くので、一覧はホームの右に置く。 */
private const val HOME_PAGE = 0
private const val LIBRARY_PAGE = 1

@Composable
private fun LauncherRoot(viewModel: LauncherViewModel, widgets: WidgetController, homePressed: Int) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val prefs = viewModel.prefs

    var menuApp by remember { mutableStateOf<AppEntry?>(null) }
    var menuWidget by remember { mutableStateOf<WidgetConfig?>(null) }
    var folder by remember { mutableStateOf<AppCategory?>(null) }
    var showHomeMenu by remember { mutableStateOf(false) }
    var showWidgetPicker by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var handledHomePress by remember { mutableIntStateOf(homePressed) }

    val pagerState = rememberPagerState(initialPage = HOME_PAGE) { 2 }
    val scope = rememberCoroutineScope()

    val launch: (AppEntry) -> Unit = { app ->
        folder = null
        AppActions.launch(context, app)
    }
    val openMenu: (AppEntry) -> Unit = { app -> menuApp = app }

    LaunchedEffect(homePressed) {
        if (homePressed == handledHomePress) return@LaunchedEffect
        handledHomePress = homePressed
        menuApp = null
        menuWidget = null
        folder = null
        showHomeMenu = false
        showWidgetPicker = false
        showSettings = false
        pagerState.animateScrollToPage(HOME_PAGE)
    }

    // ホーム画面では戻るボタンを無視し、アプリ一覧ではホームへ戻る
    BackHandler { }
    BackHandler(enabled = pagerState.currentPage != HOME_PAGE) {
        scope.launch { pagerState.animateScrollToPage(HOME_PAGE) }
    }

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        beyondViewportPageCount = 1,
    ) { page ->
        when (page) {
            HOME_PAGE -> HomeScreen(
                state = state,
                widgetController = widgets,
                onAppClick = launch,
                onAppLongClick = openMenu,
                onWidgetLongPress = { menuWidget = it },
                onSwipeDown = { AppActions.expandNotifications(context) },
                onBackgroundLongPress = { showHomeMenu = true },
            )
            else -> AppLibraryScreen(
                state = state,
                onAppClick = launch,
                onAppLongClick = openMenu,
                onOpenFolder = { folder = it },
                onOpenSettings = { showSettings = true },
            )
        }
    }

    state.groups.firstOrNull { it.category == folder }?.let { group ->
        FolderDialog(
            group = group,
            onDismiss = { folder = null },
            onAppClick = launch,
            onAppLongClick = openMenu,
        )
    }

    menuApp?.let { app ->
        AppMenuDialog(
            app = app,
            state = state,
            prefs = prefs,
            onDismiss = { menuApp = null },
            onOpenAppInfo = { AppActions.openAppInfo(context, app) },
            onUninstall = { AppActions.uninstall(context, app) },
        )
    }

    menuWidget?.let { config ->
        WidgetMenuDialog(
            config = config,
            state = state,
            controller = widgets,
            prefs = prefs,
            onDismiss = { menuWidget = null },
        )
    }

    if (showHomeMenu) {
        HomeMenuDialog(
            onAddWidget = {
                showHomeMenu = false
                showWidgetPicker = true
            },
            onOpenSettings = {
                showHomeMenu = false
                showSettings = true
            },
            onDismiss = { showHomeMenu = false },
        )
    }

    if (showWidgetPicker) {
        WidgetPickerDialog(
            controller = widgets,
            onPick = { info ->
                showWidgetPicker = false
                widgets.add(info)
            },
            onDismiss = { showWidgetPicker = false },
        )
    }

    if (showSettings) {
        SettingsScreen(
            state = state,
            prefs = prefs,
            onOpenHomeSettings = { AppActions.openHomeSettings(context) },
            onDismiss = { showSettings = false },
        )
    }
}

@Composable
private fun HomeMenuDialog(onAddWidget: () -> Unit, onOpenSettings: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ホーム画面") },
        text = {
            Column {
                TextButton(onClick = onAddWidget) { Text("ウィジェットを追加") }
                TextButton(onClick = onOpenSettings) { Text("設定") }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("閉じる") } },
    )
}
