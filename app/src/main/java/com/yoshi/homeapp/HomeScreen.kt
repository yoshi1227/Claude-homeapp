package com.yoshi.homeapp

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val textShadow = Shadow(color = Color.Black.copy(alpha = 0.5f), blurRadius = 8f)

/**
 * ホーム画面。上にウィジェット、下にユーザーが追加したアプリを表示する。
 * 何もないところを下にスワイプすると通知パネル、長押しするとメニュー（ウィジェット追加・設定）を開く。
 * ウィジェットの上では、ウィジェット自身の操作（リストのスクロールなど）を優先する。
 */
@Composable
fun HomeScreen(
    state: LauncherUiState,
    widgetController: WidgetController,
    onAppClick: (AppEntry) -> Unit,
    onAppLongClick: (AppEntry) -> Unit,
    onWidgetLongPress: (WidgetConfig) -> Unit,
    onSwipeDown: () -> Unit,
    onBackgroundLongPress: () -> Unit,
) {
    val swipeThreshold = with(LocalDensity.current) { 64.dp.toPx() }
    val backgroundGestures = Modifier
        .pointerInput(Unit) {
            detectTapGestures(onLongPress = { onBackgroundLongPress() })
        }
        .pointerInput(Unit) {
            var total = 0f
            detectVerticalDragGestures(
                onDragStart = { total = 0f },
                onDragEnd = { if (total > swipeThreshold) onSwipeDown() },
                onVerticalDrag = { _, dragAmount -> total += dragAmount },
            )
        }

    Column(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
        Spacer(Modifier.height(16.dp).fillMaxWidth().then(backgroundGestures))
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(horizontal = 12.dp),
        ) {
            state.settings.widgets.forEach { config ->
                key(config.id) {
                    HomeWidget(
                        config = config,
                        controller = widgetController,
                        onLongPress = { onWidgetLongPress(config) },
                    )
                }
            }
        }
        Column(modifier = Modifier.weight(1f).fillMaxWidth().then(backgroundGestures)) {
            Spacer(Modifier.weight(1f))
            if (state.favorites.isEmpty()) {
                Text(
                    text = "左にスワイプでアプリ一覧\n" +
                        "アイコンを長押しして「ホームに追加」\n" +
                        "何もないところを長押しでウィジェットを追加",
                    style = TextStyle(color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp, shadow = textShadow),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                FavoritesGrid(state.favorites, onAppClick, onAppLongClick)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun FavoritesGrid(
    apps: List<AppEntry>,
    onAppClick: (AppEntry) -> Unit,
    onAppLongClick: (AppEntry) -> Unit,
) {
    val columns = 4
    Column(
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier.padding(horizontal = 24.dp),
    ) {
        apps.chunked(columns).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                for (i in 0 until columns) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        row.getOrNull(i)?.let { app ->
                            AppIcon(
                                app = app,
                                size = 60.dp,
                                onClick = { onAppClick(app) },
                                onLongClick = { onAppLongClick(app) },
                            )
                        }
                    }
                }
            }
        }
    }
}
