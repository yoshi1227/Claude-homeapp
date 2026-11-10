package com.yoshi.homeapp

import android.annotation.SuppressLint
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.os.SystemClock
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.ViewConfiguration
import kotlin.math.abs

/** ウィジェットを長押しでメニューを開けるようにした [AppWidgetHost]。 */
class LauncherWidgetHost(context: Context) : AppWidgetHost(context, HOST_ID) {

    override fun onCreateView(context: Context, appWidgetId: Int, appWidget: AppWidgetProviderInfo?): AppWidgetHostView =
        LongPressWidgetHostView(context)

    companion object {
        const val HOST_ID = 1024
    }
}

/**
 * ウィジェット内のボタンやリストの操作はそのまま通しつつ、指を動かさずに長押ししたときだけ
 * [onLongPress] を呼ぶ。長押しが成立したらウィジェットにはキャンセルを送る。
 */
class LongPressWidgetHostView(context: Context) : AppWidgetHostView(context) {

    var onLongPress: (() -> Unit)? = null

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var downX = 0f
    private var downY = 0f
    private var longPressed = false

    private val longPressRunnable = Runnable {
        longPressed = true
        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        cancelChildTouch()
        onLongPress?.invoke()
    }

    private fun cancelChildTouch() {
        val now = SystemClock.uptimeMillis()
        val cancel = MotionEvent.obtain(now, now, MotionEvent.ACTION_CANCEL, 0f, 0f, 0)
        super.dispatchTouchEvent(cancel)
        cancel.recycle()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                longPressed = false
                downX = ev.x
                downY = ev.y
                postDelayed(longPressRunnable, ViewConfiguration.getLongPressTimeout().toLong())
            }
            MotionEvent.ACTION_MOVE ->
                if (abs(ev.x - downX) > touchSlop || abs(ev.y - downY) > touchSlop) {
                    removeCallbacks(longPressRunnable)
                }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_POINTER_DOWN ->
                removeCallbacks(longPressRunnable)
        }
        if (longPressed) return true
        super.dispatchTouchEvent(ev)
        // 続くイベント（指の移動・離す）も受け取って長押し判定を取り消せるよう、常に処理済みにする
        return true
    }
}
