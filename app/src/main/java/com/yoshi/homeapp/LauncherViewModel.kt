package com.yoshi.homeapp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class CategoryGroup(val category: AppCategory, val apps: List<AppEntry>)

data class LauncherUiState(
    val favorites: List<AppEntry> = emptyList(),
    val groups: List<CategoryGroup> = emptyList(),
    val hiddenApps: List<AppEntry> = emptyList(),
    val settings: LauncherSettings = LauncherSettings(),
) {
    fun categoryOf(app: AppEntry): AppCategory =
        settings.categoryOverrides[app.key] ?: app.detectedCategory

    fun isFavorite(app: AppEntry) = app.key in settings.favorites
}

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppRepository(application)
    val prefs = LauncherPrefs(application)

    val uiState: StateFlow<LauncherUiState> =
        combine(repository.apps, prefs.settings) { apps, settings ->
            val visible = apps.filter { it.key !in settings.hidden }
            val byKey = visible.associateBy { it.key }
            LauncherUiState(
                favorites = settings.favorites.mapNotNull { byKey[it] },
                groups = visible
                    .groupBy { settings.categoryOverrides[it.key] ?: it.detectedCategory }
                    .toSortedMap(compareBy { it.ordinal })
                    .map { (category, list) -> CategoryGroup(category, list) },
                hiddenApps = apps.filter { it.key in settings.hidden },
                settings = settings,
            )
        }.stateIn(viewModelScope, SharingStarted.Eagerly, LauncherUiState(settings = prefs.settings.value))

    init {
        repository.start()
    }

    fun refresh() = repository.reload()

    override fun onCleared() {
        repository.stop()
    }
}
