package com.yoshi.homeapp

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** アイコン長押しで開くメニュー。アイコンに名前が無いので、ここでアプリ名を確認できる。 */
@Composable
fun AppMenuDialog(
    app: AppEntry,
    state: LauncherUiState,
    prefs: LauncherPrefs,
    onDismiss: () -> Unit,
    onOpenAppInfo: () -> Unit,
    onUninstall: () -> Unit,
) {
    var choosingCategory by remember { mutableStateOf(false) }
    if (choosingCategory) {
        CategoryDialog(
            app = app,
            current = state.settings.categoryOverrides[app.key],
            onSelect = { category ->
                prefs.setCategory(app.key, category)
                onDismiss()
            },
            onDismiss = onDismiss,
        )
        return
    }

    val isFavorite = state.isFavorite(app)
    val favoriteIndex = state.settings.favorites.indexOf(app.key)
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { AppIconImage(app = app, size = 48.dp) },
        title = { Text(app.label) },
        text = {
            Column {
                Text(
                    text = "カテゴリー: ${state.categoryOf(app).label}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                MenuItem(if (isFavorite) "ホームから外す" else "ホームに追加") {
                    prefs.setFavorite(app.key, !isFavorite)
                    onDismiss()
                }
                if (isFavorite) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            enabled = favoriteIndex > 0,
                            onClick = { prefs.moveFavorite(app.key, -1) },
                        ) { Text("◀ 前へ") }
                        TextButton(
                            enabled = favoriteIndex in 0 until state.settings.favorites.lastIndex,
                            onClick = { prefs.moveFavorite(app.key, 1) },
                        ) { Text("後ろへ ▶") }
                    }
                }
                MenuItem("カテゴリーを変更") { choosingCategory = true }
                MenuItem("非表示にする") {
                    prefs.setHidden(app.key, true)
                    onDismiss()
                }
                MenuItem("アプリ情報") {
                    onOpenAppInfo()
                    onDismiss()
                }
                MenuItem("アンインストール") {
                    onUninstall()
                    onDismiss()
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("閉じる") } },
    )
}

@Composable
private fun MenuItem(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
    )
}

@Composable
private fun CategoryDialog(
    app: AppEntry,
    current: AppCategory?,
    onSelect: (AppCategory?) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${app.label} のカテゴリー") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                CategoryOption("自動（${app.detectedCategory.label}）", current == null) { onSelect(null) }
                AppCategory.entries.forEach { category ->
                    CategoryOption(category.label, current == category) { onSelect(category) }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("キャンセル") } },
    )
}

@Composable
private fun CategoryOption(text: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(text)
    }
}
