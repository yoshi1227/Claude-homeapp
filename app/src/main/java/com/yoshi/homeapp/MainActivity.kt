package com.yoshi.homeapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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

    /** ホームボタンが押されるたびに増える。ホーム画面へ戻す合図に使う。 */
    private var homePressed by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                LauncherRoot(viewModel, homePressed)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.hasCategory(Intent.CATEGORY_HOME)) homePressed++
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }
}

private enum class Page { HOME, LIBRARY }

@Composable
private fun LauncherRoot(viewModel: LauncherViewModel, homePressed: Int) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val prefs = viewModel.prefs

    var menuApp by remember { mutableStateOf<AppEntry?>(null) }
    var folder by remember { mutableStateOf<AppCategory?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    var handledHomePress by remember { mutableIntStateOf(homePressed) }

    val launch: (AppEntry) -> Unit = { app ->
        folder = null
        AppActions.launch(context, app)
    }
    val openMenu: (AppEntry) -> Unit = { app -> menuApp = app }

    // 右スワイプ（指を左→右）でアプリ一覧を出すには、一覧をホームの「左」に置く
    val swipeRight = state.settings.swipeRightForLibrary
    val pages = if (swipeRight) listOf(Page.LIBRARY, Page.HOME) else listOf(Page.HOME, Page.LIBRARY)
    val homeIndex = pages.indexOf(Page.HOME)

    key(swipeRight) {
        val pagerState = rememberPagerState(initialPage = homeIndex) { pages.size }
        val scope = rememberCoroutineScope()

        LaunchedEffect(homePressed) {
            if (homePressed == handledHomePress) return@LaunchedEffect
            handledHomePress = homePressed
            menuApp = null
            folder = null
            showSettings = false
            pagerState.animateScrollToPage(homeIndex)
        }

        // ホーム画面では戻るボタンを無視し、アプリ一覧ではホームへ戻る
        BackHandler { }
        BackHandler(enabled = pagerState.currentPage != homeIndex) {
            scope.launch { pagerState.animateScrollToPage(homeIndex) }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 1,
        ) { index ->
            when (pages[index]) {
                Page.HOME -> HomeScreen(
                    state = state,
                    onAppClick = launch,
                    onAppLongClick = openMenu,
                    onSwipeDown = { AppActions.expandNotifications(context) },
                    onOpenSettings = { showSettings = true },
                )
                Page.LIBRARY -> AppLibraryScreen(
                    state = state,
                    onAppClick = launch,
                    onAppLongClick = openMenu,
                    onOpenFolder = { folder = it },
                    onOpenSettings = { showSettings = true },
                )
            }
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

    if (showSettings) {
        SettingsScreen(
            state = state,
            prefs = prefs,
            onOpenHomeSettings = { AppActions.openHomeSettings(context) },
            onDismiss = { showSettings = false },
        )
    }
}
