package com.yoshi.homeapp

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import java.util.Locale

/** アプリ名の最初の 1 文字（絵文字や漢字もそのまま 1 文字として扱う）。 */
fun firstLetter(label: String): String {
    val text = label.trim()
    if (text.isEmpty()) return "?"
    return String(Character.toChars(text.codePointAt(0))).uppercase(Locale.getDefault())
}

/** パッケージ名から決まる、落ち着いた色。同じアプリは常に同じ色になる。 */
fun iconColor(seed: String): Color {
    val hue = ((seed.hashCode().toLong() and 0xffffffffL) % 360).toFloat()
    return Color.hsv(hue, 0.45f, 0.62f)
}

/** アプリ名の最初の文字だけが書かれた簡素なアイコン。 */
@Composable
fun LetterIcon(label: String, colorSeed: String, size: Dp, modifier: Modifier = Modifier) {
    val fontSize = with(LocalDensity.current) { (size * 0.46f).toSp() }
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.28f))
            .background(iconColor(colorSeed)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = remember(label) { firstLetter(label) },
            color = Color.White,
            fontSize = fontSize,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
    }
}

/** タップで起動、長押しでメニューを開くアプリアイコン（名前は表示しない）。 */
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
    LetterIcon(
        label = app.label,
        colorSeed = app.packageName,
        size = size,
        modifier = modifier
            .clip(RoundedCornerShape(size * 0.28f))
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                },
            )
            .semantics { contentDescription = app.label },
    )
}
