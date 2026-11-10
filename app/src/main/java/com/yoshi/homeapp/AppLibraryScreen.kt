package com.yoshi.homeapp

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

private val cardShape = RoundedCornerShape(24.dp)
private val cardColor = Color.White.copy(alpha = 0.14f)

/**
 * iPhone の App ライブラリ風のアプリ一覧。カテゴリーごとのフォルダを 2 列で並べる。
 * フォルダには最大 3 個のアイコンを直接表示し、残りは右下の小さなまとめ表示をタップして開く。
 */
@Composable
fun AppLibraryScreen(
    state: LauncherUiState,
    onAppClick: (AppEntry) -> Unit,
    onAppLongClick: (AppEntry) -> Unit,
    onOpenFolder: (AppCategory) -> Unit,
    onOpenSettings: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .safeDrawingPadding(),
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "アプリ",
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "設定", tint = Color.White)
                    }
                }
            }
            items(state.groups, key = { it.category.name }) { group ->
                CategoryCard(
                    group = group,
                    onAppClick = onAppClick,
                    onAppLongClick = onAppLongClick,
                    onOpenFolder = { onOpenFolder(group.category) },
                )
            }
        }
    }
}

@Composable
private fun CategoryCard(
    group: CategoryGroup,
    onAppClick: (AppEntry) -> Unit,
    onAppLongClick: (AppEntry) -> Unit,
    onOpenFolder: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(cardShape)
                .background(cardColor)
                .clickable(onClick = onOpenFolder)
                .padding(12.dp),
        ) {
            val gap = 10.dp
            val iconSize = (maxWidth - gap) / 2
            // 4 個以下なら全部そのまま、5 個以上なら 3 個 + まとめ表示
            val direct = if (group.apps.size <= 4) group.apps else group.apps.take(3)
            val rest = if (group.apps.size <= 4) emptyList() else group.apps.drop(3)
            Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                for (row in 0 until 2) {
                    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                        for (col in 0 until 2) {
                            val index = row * 2 + col
                            val app = direct.getOrNull(index)
                            when {
                                app != null -> AppIcon(
                                    app = app,
                                    size = iconSize,
                                    onClick = { onAppClick(app) },
                                    onLongClick = { onAppLongClick(app) },
                                )
                                index == 3 && rest.isNotEmpty() -> MiniGrid(rest, iconSize, onOpenFolder)
                                else -> Spacer(Modifier.size(iconSize))
                            }
                        }
                    }
                }
            }
        }
        Text(
            text = group.category.label,
            color = Color.White,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

/** フォルダ右下の、残りのアプリを小さく並べた表示。タップでフォルダを開く。 */
@Composable
private fun MiniGrid(apps: List<AppEntry>, size: Dp, onClick: () -> Unit) {
    val gap = 4.dp
    val small = (size - gap) / 2
    Column(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.28f))
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(gap),
    ) {
        for (row in 0 until 2) {
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                for (col in 0 until 2) {
                    val app = apps.getOrNull(row * 2 + col)
                    if (app != null) {
                        AppIconImage(app = app, size = small)
                    } else {
                        Spacer(Modifier.size(small))
                    }
                }
            }
        }
    }
}

/** カテゴリーの全アプリを表示するフォルダ。 */
@Composable
fun FolderDialog(
    group: CategoryGroup,
    onDismiss: () -> Unit,
    onAppClick: (AppEntry) -> Unit,
    onAppLongClick: (AppEntry) -> Unit,
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color(0xEE262626),
            modifier = Modifier.padding(24.dp).fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = group.category.label,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.heightIn(max = 480.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    items(group.apps, key = { it.key }) { app ->
                        Box(contentAlignment = Alignment.Center) {
                            AppIcon(
                                app = app,
                                size = 56.dp,
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
