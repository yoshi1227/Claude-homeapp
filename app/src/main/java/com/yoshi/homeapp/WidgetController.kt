package com.yoshi.homeapp

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts

/**
 * ウィジェットの追加（ID の確保 → バインドの許可 → 設定画面）と削除を行う。
 * Activity の onCreate で生成すること。
 */
class WidgetController(
    private val activity: ComponentActivity,
    private val prefs: LauncherPrefs,
) {
    val host = LauncherWidgetHost(activity)
    val manager: AppWidgetManager = AppWidgetManager.getInstance(activity)

    private var pendingId = AppWidgetManager.INVALID_APPWIDGET_ID
    private var pendingInfo: AppWidgetProviderInfo? = null

    private val bindLauncher =
        activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val info = pendingInfo
            if (result.resultCode == Activity.RESULT_OK && info != null) configureOrAdd(pendingId, info) else cancelPending()
        }

    init {
        removeStaleWidgets()
    }

    fun startListening() = runCatching { host.startListening() }
    fun stopListening() = runCatching { host.stopListening() }

    fun add(info: AppWidgetProviderInfo) {
        val id = host.allocateAppWidgetId()
        pendingId = id
        pendingInfo = info
        if (manager.bindAppWidgetIdIfAllowed(id, info.profile, info.provider, null)) {
            configureOrAdd(id, info)
        } else {
            // 初回はユーザーに「ウィジェットの作成を許可」してもらう
            bindLauncher.launch(
                Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.provider)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER_PROFILE, info.profile),
            )
        }
    }

    fun remove(id: Int) {
        host.deleteAppWidgetId(id)
        prefs.removeWidget(id)
    }

    /** Activity.onActivityResult から呼ぶ。設定画面の結果を処理したら true。 */
    fun onActivityResult(requestCode: Int, resultCode: Int): Boolean {
        if (requestCode != REQUEST_CONFIGURE) return false
        val info = pendingInfo
        if (resultCode == Activity.RESULT_OK && info != null) finishAdd(pendingId, info) else cancelPending()
        return true
    }

    private fun configureOrAdd(id: Int, info: AppWidgetProviderInfo) {
        val configurationOptional = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            (info.widgetFeatures and AppWidgetProviderInfo.WIDGET_FEATURE_CONFIGURATION_OPTIONAL) != 0
        if (info.configure == null || configurationOptional) {
            finishAdd(id, info)
            return
        }
        try {
            host.startAppWidgetConfigureActivityForResult(activity, id, 0, REQUEST_CONFIGURE, null)
        } catch (e: Exception) {
            finishAdd(id, info)
        }
    }

    private fun finishAdd(id: Int, info: AppWidgetProviderInfo) {
        prefs.addWidget(id, defaultHeightDp(info))
        pendingId = AppWidgetManager.INVALID_APPWIDGET_ID
        pendingInfo = null
    }

    private fun cancelPending() {
        if (pendingId != AppWidgetManager.INVALID_APPWIDGET_ID) host.deleteAppWidgetId(pendingId)
        pendingId = AppWidgetManager.INVALID_APPWIDGET_ID
        pendingInfo = null
        Toast.makeText(activity, "ウィジェットを追加できませんでした", Toast.LENGTH_SHORT).show()
    }

    /** アンインストールされたアプリのウィジェットや、保存されていない ID を片付ける。 */
    private fun removeStaleWidgets() {
        val saved = prefs.settings.value.widgets.map { it.id }.toSet()
        host.appWidgetIds.filter { it !in saved }.forEach { host.deleteAppWidgetId(it) }
        saved.filter { manager.getAppWidgetInfo(it) == null }.forEach { remove(it) }
    }

    private fun defaultHeightDp(info: AppWidgetProviderInfo): Int {
        val density = activity.resources.displayMetrics.density
        val minHeight = (info.minHeight / density).toInt()
        val cellHeight = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) info.targetCellHeight * CELL_HEIGHT_DP else 0
        return maxOf(minHeight, cellHeight, MIN_HEIGHT_DP).coerceAtMost(MAX_HEIGHT_DP)
    }

    companion object {
        const val REQUEST_CONFIGURE = 4201
        const val CELL_HEIGHT_DP = 100
        const val MIN_HEIGHT_DP = 60
        const val MAX_HEIGHT_DP = 800
        const val HEIGHT_STEP_DP = 40
    }
}
