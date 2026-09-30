package com.yoshi.homeapp

import android.content.ComponentName
import android.os.UserHandle

/** ランチャーに表示する 1 アクティビティ。[key] は設定の保存に使う一意なキー。 */
data class AppEntry(
    val key: String,
    val label: String,
    val component: ComponentName,
    val user: UserHandle,
    val detectedCategory: AppCategory,
) {
    val packageName: String get() = component.packageName
}
