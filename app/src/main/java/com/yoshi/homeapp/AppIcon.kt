package com.yoshi.homeapp

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp

/** アプリ本来のアイコン（名前は表示しない）。 */
@Composable
fun AppIconImage(app: AppEntry, size: Dp, modifier: Modifier = Modifier) {
    Image(
        bitmap = app.icon,
        contentDescription = app.label,
        modifier = modifier.size(size),
    )
}

/** タップで起動、長押しでメニューを開くアプリアイコン。 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppIcon(
    app: AppEntry,
    size: Dp,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    AppIconImage(
        app = app,
        size = size,
        modifier = modifier
            .clip(RoundedCornerShape(size * 0.3f))
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                },
            ),
    )
}
