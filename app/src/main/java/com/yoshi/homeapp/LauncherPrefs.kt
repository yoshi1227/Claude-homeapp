package com.yoshi.homeapp

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** ホーム画面に置いたウィジェット。 */
data class WidgetConfig(val id: Int, val heightDp: Int)

data class LauncherSettings(
    /** 非表示にしたアプリのキー。 */
    val hidden: Set<String> = emptySet(),
    /** ホーム画面に置いたアプリのキー（並び順どおり）。 */
    val favorites: List<String> = emptyList(),
    /** ユーザーが手動で変更したカテゴリー。 */
    val categoryOverrides: Map<String, AppCategory> = emptyMap(),
    /** ホーム画面のウィジェット（上から順）。 */
    val widgets: List<WidgetConfig> = emptyList(),
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
        widgets = prefs.getString(KEY_WIDGETS, "").orEmpty()
            .split(SEPARATOR)
            .mapNotNull { entry ->
                val (id, height) = entry.split(':').takeIf { it.size == 2 } ?: return@mapNotNull null
                WidgetConfig(id.toIntOrNull() ?: return@mapNotNull null, height.toIntOrNull() ?: return@mapNotNull null)
            },
    )

    private fun update(transform: (LauncherSettings) -> LauncherSettings) {
        val next = transform(_settings.value)
        prefs.edit()
            .putStringSet(KEY_HIDDEN, next.hidden)
            .putString(KEY_FAVORITES, next.favorites.joinToString(SEPARATOR))
            .putStringSet(KEY_CATEGORIES, next.categoryOverrides.map { (k, v) -> "$k=${v.name}" }.toSet())
            .putString(KEY_WIDGETS, next.widgets.joinToString(SEPARATOR) { "${it.id}:${it.heightDp}" })
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

    fun addWidget(id: Int, heightDp: Int) = update {
        it.copy(widgets = it.widgets.filter { w -> w.id != id } + WidgetConfig(id, heightDp))
    }

    fun removeWidget(id: Int) = update { it.copy(widgets = it.widgets.filter { w -> w.id != id }) }

    fun setWidgetHeight(id: Int, heightDp: Int) = update {
        it.copy(widgets = it.widgets.map { w -> if (w.id == id) w.copy(heightDp = heightDp) else w })
    }

    fun moveWidget(id: Int, offset: Int) = update {
        val list = it.widgets.toMutableList()
        val from = list.indexOfFirst { w -> w.id == id }
        val to = from + offset
        if (from < 0 || to !in list.indices) return@update it
        list.add(to, list.removeAt(from))
        it.copy(widgets = list)
    }

    private companion object {
        const val KEY_HIDDEN = "hidden"
        const val KEY_FAVORITES = "favorites"
        const val KEY_CATEGORIES = "category_overrides"
        const val KEY_WIDGETS = "widgets"
        const val SEPARATOR = "\n"
    }
}
