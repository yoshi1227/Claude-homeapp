package com.yoshi.homeapp

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class LauncherSettings(
    /** 非表示にしたアプリのキー。 */
    val hidden: Set<String> = emptySet(),
    /** ホーム画面に置いたアプリのキー（並び順どおり）。 */
    val favorites: List<String> = emptyList(),
    /** ユーザーが手動で変更したカテゴリー。 */
    val categoryOverrides: Map<String, AppCategory> = emptyMap(),
    /**
     * true: 右スワイプ（指を左から右へ）でアプリ一覧を開く。
     * false: 左スワイプ（指を右から左へ）でアプリ一覧を開く。
     */
    val swipeRightForLibrary: Boolean = true,
)

/** 設定を SharedPreferences に保存する。 */
class LauncherPrefs(context: Context) {

    private val prefs = context.getSharedPreferences("launcher", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(load())
    val settings: StateFlow<LauncherSettings> = _settings

    private fun load(): LauncherSettings = LauncherSettings(
        hidden = prefs.getStringSet(KEY_HIDDEN, emptySet()).orEmpty().toSet(),
        favorites = prefs.getString(KEY_FAVORITES, "").orEmpty()
            .split(SEPARATOR).filter { it.isNotEmpty() },
        categoryOverrides = prefs.getStringSet(KEY_CATEGORIES, emptySet()).orEmpty()
            .mapNotNull { entry ->
                val index = entry.lastIndexOf('=')
                if (index <= 0) return@mapNotNull null
                val category = AppCategory.fromName(entry.substring(index + 1)) ?: return@mapNotNull null
                entry.substring(0, index) to category
            }.toMap(),
        swipeRightForLibrary = prefs.getBoolean(KEY_SWIPE_RIGHT, true),
    )

    private fun update(transform: (LauncherSettings) -> LauncherSettings) {
        val next = transform(_settings.value)
        prefs.edit()
            .putStringSet(KEY_HIDDEN, next.hidden)
            .putString(KEY_FAVORITES, next.favorites.joinToString(SEPARATOR))
            .putStringSet(KEY_CATEGORIES, next.categoryOverrides.map { (k, v) -> "$k=${v.name}" }.toSet())
            .putBoolean(KEY_SWIPE_RIGHT, next.swipeRightForLibrary)
            .apply()
        _settings.value = next
    }

    fun setHidden(key: String, hidden: Boolean) = update {
        it.copy(
            hidden = if (hidden) it.hidden + key else it.hidden - key,
            favorites = if (hidden) it.favorites - key else it.favorites,
        )
    }

    fun setFavorite(key: String, favorite: Boolean) = update {
        it.copy(favorites = if (favorite) (it.favorites - key) + key else it.favorites - key)
    }

    fun moveFavorite(key: String, offset: Int) = update {
        val list = it.favorites.toMutableList()
        val from = list.indexOf(key)
        val to = from + offset
        if (from < 0 || to !in list.indices) return@update it
        list.add(to, list.removeAt(from))
        it.copy(favorites = list)
    }

    /** [category] が null ならカテゴリーを自動判定に戻す。 */
    fun setCategory(key: String, category: AppCategory?) = update {
        it.copy(
            categoryOverrides = if (category == null) it.categoryOverrides - key
            else it.categoryOverrides + (key to category),
        )
    }

    fun resetCategories() = update { it.copy(categoryOverrides = emptyMap()) }

    fun setSwipeRightForLibrary(value: Boolean) = update { it.copy(swipeRightForLibrary = value) }

    private companion object {
        const val KEY_HIDDEN = "hidden"
        const val KEY_FAVORITES = "favorites"
        const val KEY_CATEGORIES = "category_overrides"
        const val KEY_SWIPE_RIGHT = "swipe_right_for_library"
        const val SEPARATOR = "\n"
    }
}
