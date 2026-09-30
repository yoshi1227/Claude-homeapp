package com.yoshi.homeapp

import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherApps
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import android.os.UserManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.text.Collator
import java.util.Locale

/** 端末にインストールされている起動可能なアプリの一覧を管理する。 */
class AppRepository(private val context: Context) {

    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val userManager = context.getSystemService(UserManager::class.java)
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _apps = MutableStateFlow<List<AppEntry>>(emptyList())
    val apps: StateFlow<List<AppEntry>> = _apps

    private val callback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String, user: UserHandle) = reload()
        override fun onPackageAdded(packageName: String, user: UserHandle) = reload()
        override fun onPackageChanged(packageName: String, user: UserHandle) = reload()
        override fun onPackagesAvailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) = reload()
        override fun onPackagesUnavailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) = reload()
    }

    fun start() {
        launcherApps.registerCallback(callback, mainHandler)
        reload()
    }

    fun stop() {
        launcherApps.unregisterCallback(callback)
    }

    fun reload() {
        val collator = Collator.getInstance(Locale.getDefault())
        val result = userManager.userProfiles.flatMap { user ->
            val serial = userManager.getSerialNumberForUser(user)
            launcherApps.getActivityList(null, user)
                .filter { it.componentName.packageName != context.packageName }
                .map { info ->
                    AppEntry(
                        key = keyOf(info.componentName, serial),
                        label = info.label?.toString()?.trim().orEmpty()
                            .ifEmpty { info.componentName.packageName },
                        component = info.componentName,
                        user = user,
                        detectedCategory = AppCategory.detect(info.applicationInfo),
                    )
                }
        }.sortedWith { a, b -> collator.compare(a.label, b.label) }
        _apps.value = result
    }

    private fun keyOf(component: ComponentName, userSerial: Long) =
        "${component.flattenToString()}#$userSerial"
}
