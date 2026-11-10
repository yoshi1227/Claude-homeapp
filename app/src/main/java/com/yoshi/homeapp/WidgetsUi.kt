package com.yoshi.homeapp

import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetProviderInfo
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.util.SizeF
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.Collator
import java.util.Locale

/** ホーム画面に置いたウィジェット 1 個。幅いっぱい、高さは設定値。 */
@Composable
fun HomeWidget(
    config: WidgetConfig,
    controller: WidgetController,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val info = remember(config.id) { controller.manager.getAppWidgetInfo(config.id) } ?: return
    val currentOnLongPress by rememberUpdatedState(onLongPress)
    BoxWithConstraints(modifier = modifier.fillMaxWidth().height(config.heightDp.dp)) {
        val widthDp = maxWidth.value.toInt()
        val heightDp = config.heightDp
        AndroidView(
            factory = { context ->
                controller.host.createView(context, config.id, info).apply {
                    (this as? LongPressWidgetHostView)?.onLongPress = { currentOnLongPress() }
                }
            },
            update = { view -> view.updateSizeDp(widthDp, heightDp) },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

/** ウィジェットに表示サイズを伝える（伝えないとレイアウトが崩れるウィジェットがある）。 */
private fun AppWidgetHostView.updateSizeDp(widthDp: Int, heightDp: Int) {
    val size = widthDp to heightDp
    if (tag == size) return
    tag = size
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        updateAppWidgetSize(Bundle(), listOf(SizeF(widthDp.toFloat(), heightDp.toFloat())))
    } else {
        @Suppress("DEPRECATION")
        updateAppWidgetSize(null, widthDp, heightDp, widthDp, heightDp)
    }
}

/** ウィジェット長押しのメニュー。 */
@Composable
fun WidgetMenuDialog(
    config: WidgetConfig,
    state: LauncherUiState,
    controller: WidgetController,
    prefs: LauncherPrefs,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val label = remember(config.id) {
        controller.manager.getAppWidgetInfo(config.id)?.loadLabel(context.packageManager) ?: "ウィジェット"
    }
    val index = state.settings.widgets.indexOfFirst { it.id == config.id }
    val current = state.settings.widgets.getOrNull(index) ?: config
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(label) },
        text = {
            Column {
                Text("高さ: ${current.heightDp}dp", style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        enabled = current.heightDp > WidgetController.MIN_HEIGHT_DP,
                        onClick = {
                            prefs.setWidgetHeight(
                                config.id,
                                (current.heightDp - WidgetController.HEIGHT_STEP_DP).coerceAtLeast(WidgetController.MIN_HEIGHT_DP),
                            )
                        },
                    ) { Text("小さく") }
                    TextButton(
                        enabled = current.heightDp < WidgetController.MAX_HEIGHT_DP,
                        onClick = {
                            prefs.setWidgetHeight(
                                config.id,
                                (current.heightDp + WidgetController.HEIGHT_STEP_DP).coerceAtMost(WidgetController.MAX_HEIGHT_DP),
                            )
                        },
                    ) { Text("大きく") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(enabled = index > 0, onClick = { prefs.moveWidget(config.id, -1) }) { Text("▲ 上へ") }
                    TextButton(
                        enabled = index in 0 until state.settings.widgets.lastIndex,
                        onClick = { prefs.moveWidget(config.id, 1) },
                    ) { Text("▼ 下へ") }
                }
                TextButton(onClick = {
                    controller.remove(config.id)
                    onDismiss()
                }) { Text("ホームから削除") }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("閉じる") } },
    )
}

private class WidgetChoice(
    val info: AppWidgetProviderInfo,
    val appLabel: String,
    val label: String,
    val sizeText: String,
    val preview: ImageBitmap?,
)

/** 端末にあるウィジェットの一覧から選ぶ画面。 */
@Composable
fun WidgetPickerDialog(
    controller: WidgetController,
    onPick: (AppWidgetProviderInfo) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var choices by remember { mutableStateOf<List<WidgetChoice>?>(null) }
    LaunchedEffect(Unit) {
        choices = withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val density = context.resources.displayMetrics.densityDpi
            val collator = Collator.getInstance(Locale.getDefault())
            controller.manager.installedProviders.map { info ->
                val preview = runCatching { info.loadPreviewImage(context, density) ?: info.loadIcon(context, density) }
                    .getOrNull()
                WidgetChoice(
                    info = info,
                    appLabel = info.activityInfo.applicationInfo.loadLabel(pm).toString(),
                    label = info.loadLabel(pm),
                    sizeText = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        "${info.targetCellWidth}×${info.targetCellHeight}"
                    } else {
                        ""
                    },
                    preview = preview?.let { previewBitmap(it) },
                )
            }.sortedWith { a, b ->
                collator.compare(a.appLabel, b.appLabel).takeIf { it != 0 } ?: collator.compare(a.label, b.label)
            }
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 20.dp, end = 8.dp, top = 12.dp),
                ) {
                    Text("ウィジェットを追加", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text("閉じる") }
                }
                val list = choices
                if (list == null) {
                    Text("読み込み中…", modifier = Modifier.padding(20.dp))
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        var lastApp: String? = null
                        val rows = list.map { choice ->
                            val header = if (choice.appLabel != lastApp) choice.appLabel else null
                            lastApp = choice.appLabel
                            header to choice
                        }
                        items(rows) { (header, choice) ->
                            Column {
                                if (header != null) {
                                    Text(
                                        text = header,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 4.dp),
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onPick(choice.info) }
                                        .padding(horizontal = 20.dp, vertical = 10.dp),
                                ) {
                                    choice.preview?.let {
                                        Image(
                                            bitmap = it,
                                            contentDescription = null,
                                            contentScale = ContentScale.Fit,
                                            modifier = Modifier.widthIn(max = 160.dp).heightIn(max = 120.dp),
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(choice.label, style = MaterialTheme.typography.bodyLarge)
                                        if (choice.sizeText.isNotEmpty()) {
                                            Text(choice.sizeText, style = MaterialTheme.typography.bodySmall)
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
}

private fun previewBitmap(drawable: Drawable): ImageBitmap? {
    val maxPx = 480
    val w = drawable.intrinsicWidth.takeIf { it > 0 } ?: 200
    val h = drawable.intrinsicHeight.takeIf { it > 0 } ?: 200
    val scale = minOf(1f, maxPx.toFloat() / maxOf(w, h))
    return runCatching {
        drawable.toBitmap((w * scale).toInt().coerceAtLeast(1), (h * scale).toInt().coerceAtLeast(1)).asImageBitmap()
    }.getOrNull()
}
