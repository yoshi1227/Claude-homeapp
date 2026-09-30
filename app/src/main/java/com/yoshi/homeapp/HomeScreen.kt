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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val textShadow = Shadow(color = Color.Black.copy(alpha = 0.5f), blurRadius = 8f)

/**
 * ホーム画面。時計と、ユーザーが追加したアプリだけを表示する。
 * 下スワイプでアプリ一覧は開かない（通知パネルを開く）。背景の長押しで設定を開く。
 */
@Composable
fun HomeScreen(
    state: LauncherUiState,
    onAppClick: (AppEntry) -> Unit,
    onAppLongClick: (AppEntry) -> Unit,
    onSwipeDown: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val swipeThreshold = with(LocalDensity.current) { 64.dp.toPx() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onLongPress = { onOpenSettings() })
            }
            .pointerInput(Unit) {
                var total = 0f
                detectVerticalDragGestures(
                    onDragStart = { total = 0f },
                    onDragEnd = { if (total > swipeThreshold) onSwipeDown() },
                    onVerticalDrag = { _, dragAmount -> total += dragAmount },
                )
            }
            .safeDrawingPadding(),
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
            Spacer(Modifier.height(48.dp))
            Clock()
            Spacer(Modifier.weight(1f))
            if (state.favorites.isEmpty()) {
                val direction = if (state.settings.swipeRightForLibrary) "右" else "左"
                Text(
                    text = "${direction}にスワイプでアプリ一覧\nアイコンを長押しして「ホームに追加」",
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
private fun Clock() {
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            delay(((60 - now.second) * 1000L).coerceAtLeast(1000L))
        }
    }
    val timeFormat = remember { DateTimeFormatter.ofPattern("H:mm") }
    val dateFormat = remember { DateTimeFormatter.ofPattern("M月d日(E)", Locale.JAPAN) }
    Column {
        Text(
            text = now.format(timeFormat),
            style = TextStyle(color = Color.White, fontSize = 72.sp, fontWeight = FontWeight.Light, shadow = textShadow),
        )
        Text(
            text = now.format(dateFormat),
            style = TextStyle(color = Color.White, fontSize = 18.sp, shadow = textShadow),
        )
    }
}

@Composable
private fun FavoritesGrid(
    apps: List<AppEntry>,
    onAppClick: (AppEntry) -> Unit,
    onAppLongClick: (AppEntry) -> Unit,
) {
    val columns = 4
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
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
