package com.yoshi.homeapp

import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import android.os.UserManager
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.Collator
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/** 端末にインストールされている起動可能なアプリの一覧を管理する。 */
class AppRepository(private val context: Context, private val scope: CoroutineScope) {

    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val userManager = context.getSystemService(UserManager::class.java)
    private val mainHandler = Handler(Looper.getMainLooper())

    /** アイコンは読み込みが重いのでキャッシュする。アプリが更新されたら破棄する。 */
    private val iconCache = ConcurrentHashMap<String, ImageBitmap>()
    private val iconSizePx = (context.resources.displayMetrics.density * 72).toInt()
    private var reloadJob: Job? = null

    private val _apps = MutableStateFlow<List<AppEntry>>(emptyList())
    val apps: StateFlow<List<AppEntry>> = _apps

    private val callback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String, user: UserHandle) = changed(packageName)
        override fun onPackageAdded(packageName: String, user: UserHandle) = changed(packageName)
        override fun onPackageChanged(packageName: String, user: UserHandle) = changed(packageName)
        override fun onPackagesAvailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) =
            packageNames.forEach { changed(it) }
        override fun onPackagesUnavailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) =
            packageNames.forEach { changed(it) }
    }

    fun start() {
        launcherApps.registerCallback(callback, mainHandler)
        reload()
    }

    fun stop() {
        launcherApps.unregisterCallback(callback)
    }

    private fun changed(packageName: String) {
        iconCache.keys.removeAll { it.startsWith("$packageName/") }
        reload()
    }

    fun reload() {
        reloadJob?.cancel()
        reloadJob = scope.launch {
            _apps.value = withContext(Dispatchers.IO) { load() }
        }
    }

    private fun load(): List<AppEntry> {
        val collator = Collator.getInstance(Locale.getDefault())
        return userManager.userProfiles.flatMap { user ->
            val serial = userManager.getSerialNumberForUser(user)
            launcherApps.getActivityList(null, user)
                .filter { it.componentName.packageName != context.packageName }
                .map { info ->
                    val key = keyOf(info.componentName, serial)
                    AppEntry(
                        key = key,
                        label = info.label?.toString()?.trim().orEmpty()
                            .ifEmpty { info.componentName.packageName },
                        component = info.componentName,
                        user = user,
                        detectedCategory = AppCategory.detect(info.applicationInfo),
                        icon = iconCache.getOrPut(key) { loadIcon(info) },
                    )
                }
        }.sortedWith { a, b -> collator.compare(a.label, b.label) }
    }

    /** アプリ本来のアイコン（仕事用プロファイルのバッジ付き）。 */
    private fun loadIcon(info: LauncherActivityInfo): ImageBitmap =
        info.getBadgedIcon(0).toBitmap(iconSizePx, iconSizePx).asImageBitmap()

    private fun keyOf(component: ComponentName, userSerial: Long) =
        "${component.flattenToString()}#$userSerial"
}
