package com.yoshi.homeapp

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun SettingsScreen(
    state: LauncherUiState,
    prefs: LauncherPrefs,
    onOpenHomeSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().safeDrawingPadding(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 20.dp, end = 8.dp, top = 12.dp),
                    ) {
                        Text("設定", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
                        TextButton(onClick = onDismiss) { Text("閉じる") }
                    }
                }

                item { SectionTitle("アプリ一覧を開くスワイプ") }
                item {
                    RadioRow("右スワイプ（指を左から右へ）", state.settings.swipeRightForLibrary) {
                        prefs.setSwipeRightForLibrary(true)
                    }
                }
                item {
                    RadioRow("左スワイプ（指を右から左へ）", !state.settings.swipeRightForLibrary) {
                        prefs.setSwipeRightForLibrary(false)
                    }
                }

                item { HorizontalDivider(Modifier.padding(vertical = 8.dp)) }
                item { SectionTitle("非表示のアプリ（${state.hiddenApps.size}）") }
                if (state.hiddenApps.isEmpty()) {
                    item {
                        Text(
                            "アイコンを長押しして「非表示にする」を選ぶと、ここに表示されます。",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(horizontal = 20.dp),
                        )
                    }
                }
                items(state.hiddenApps, key = { it.key }) { app ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
                    ) {
                        LetterIcon(label = app.label, colorSeed = app.packageName, size = 40.dp)
                        Text(app.label, modifier = Modifier.weight(1f))
                        OutlinedButton(onClick = { prefs.setHidden(app.key, false) }) { Text("表示する") }
                    }
                }

                item { HorizontalDivider(Modifier.padding(vertical = 8.dp)) }
                item { SectionTitle("その他") }
                item {
                    TextButton(
                        onClick = { prefs.resetCategories() },
                        enabled = state.settings.categoryOverrides.isNotEmpty(),
                        modifier = Modifier.padding(horizontal = 8.dp),
                    ) { Text("変更したカテゴリーをすべて自動に戻す") }
                }
                item {
                    TextButton(
                        onClick = onOpenHomeSettings,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    ) { Text("デフォルトのホームアプリを変更") }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
    )
}

@Composable
private fun RadioRow(text: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 8.dp),
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(text)
    }
}
