package com.yoshi.homeapp

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.net.Uri
import android.provider.Settings
import android.widget.Toast

/** アプリの起動やシステム画面の呼び出し。 */
object AppActions {

    fun launch(context: Context, app: AppEntry) {
        try {
            context.getSystemService(LauncherApps::class.java)
                .startMainActivity(app.component, app.user, null, null)
        } catch (e: Exception) {
            Toast.makeText(context, "${app.label} を起動できません", Toast.LENGTH_SHORT).show()
        }
    }

    fun openAppInfo(context: Context, app: AppEntry) {
        try {
            context.getSystemService(LauncherApps::class.java)
                .startAppDetailsActivity(app.component, app.user, null, null)
        } catch (e: Exception) {
            Toast.makeText(context, "アプリ情報を開けません", Toast.LENGTH_SHORT).show()
        }
    }

    fun uninstall(context: Context, app: AppEntry) {
        val intent = Intent(Intent.ACTION_DELETE, Uri.fromParts("package", app.packageName, null))
            .putExtra(Intent.EXTRA_USER, app.user)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            openAppInfo(context, app)
        }
    }

    fun openHomeSettings(context: Context) {
        try {
            context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: ActivityNotFoundException) {
            context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    /** 通知パネルを開く（非公開 API のため失敗しても何もしない）。 */
    @SuppressLint("WrongConstant")
    fun expandNotifications(context: Context) {
        try {
            val service = context.getSystemService("statusbar") ?: return
            Class.forName("android.app.StatusBarManager")
                .getMethod("expandNotificationsPanel")
                .invoke(service)
        } catch (_: Throwable) {
        }
    }
}
