package com.yoshi.homeapp

import android.content.pm.ApplicationInfo

/** アプリ一覧で使うカテゴリー（表示順）。 */
enum class AppCategory(val label: String) {
    SOCIAL("SNS・連絡"),
    PRODUCTIVITY("仕事効率化"),
    MAPS("地図・ナビ"),
    NEWS("ニュース・読書"),
    IMAGE("写真・カメラ"),
    VIDEO("動画"),
    AUDIO("音楽・オーディオ"),
    GAME("ゲーム"),
    SHOPPING("買い物・決済"),
    UTILITY("ツール"),
    ACCESSIBILITY("ユーザー補助"),
    OTHER("その他");

    companion object {
        fun fromName(name: String?): AppCategory? = entries.firstOrNull { it.name == name }

        /**
         * アプリが自己申告している [ApplicationInfo.category] を元にカテゴリーを決める。
         * 未申告のアプリはパッケージ名から推測し、それでも分からなければシステムアプリは「ツール」、
         * それ以外は「その他」にする。
         */
        fun detect(info: ApplicationInfo): AppCategory {
            when (info.category) {
                ApplicationInfo.CATEGORY_GAME -> return GAME
                ApplicationInfo.CATEGORY_AUDIO -> return AUDIO
                ApplicationInfo.CATEGORY_VIDEO -> return VIDEO
                ApplicationInfo.CATEGORY_IMAGE -> return IMAGE
                ApplicationInfo.CATEGORY_SOCIAL -> return SOCIAL
                ApplicationInfo.CATEGORY_NEWS -> return NEWS
                ApplicationInfo.CATEGORY_MAPS -> return MAPS
                ApplicationInfo.CATEGORY_PRODUCTIVITY -> return PRODUCTIVITY
                ApplicationInfo.CATEGORY_ACCESSIBILITY -> return ACCESSIBILITY
            }
            guessFromPackage(info.packageName)?.let { return it }
            val isSystem = (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            return if (isSystem) UTILITY else OTHER
        }

        private val packageHints: List<Pair<String, AppCategory>> = listOf(
            // 連絡
            "com.google.android.dialer" to SOCIAL,
            "com.google.android.apps.messaging" to SOCIAL,
            "com.google.android.gm" to SOCIAL,
            "com.google.android.contacts" to SOCIAL,
            "jp.naver.line" to SOCIAL,
            "com.whatsapp" to SOCIAL,
            "com.discord" to SOCIAL,
            "com.slack" to PRODUCTIVITY,
            "com.twitter" to SOCIAL,
            "com.instagram" to SOCIAL,
            "com.facebook" to SOCIAL,
            // 仕事効率化
            "com.google.android.calendar" to PRODUCTIVITY,
            "com.google.android.apps.docs" to PRODUCTIVITY,
            "com.google.android.keep" to PRODUCTIVITY,
            "com.google.android.apps.tasks" to PRODUCTIVITY,
            "com.microsoft.office" to PRODUCTIVITY,
            "com.microsoft.teams" to PRODUCTIVITY,
            "com.notion" to PRODUCTIVITY,
            "com.android.chrome" to PRODUCTIVITY,
            "com.google.android.googlequicksearchbox" to PRODUCTIVITY,
            // 地図
            "com.google.android.apps.maps" to MAPS,
            "jp.co.yahoo.android.apps.transit" to MAPS,
            // 写真
            "com.google.android.GoogleCamera" to IMAGE,
            "com.google.android.apps.photos" to IMAGE,
            // 動画・音楽
            "com.google.android.youtube" to VIDEO,
            "com.netflix" to VIDEO,
            "com.amazon.avod" to VIDEO,
            "com.google.android.apps.youtube.music" to AUDIO,
            "com.spotify" to AUDIO,
            // 買い物・決済
            "com.amazon.mShop" to SHOPPING,
            "com.google.android.apps.walletnfcrel" to SHOPPING,
            "jp.ne.paypay" to SHOPPING,
            "jp.co.rakuten" to SHOPPING,
            "com.android.vending" to SHOPPING,
            // ツール
            "com.google.android.deskclock" to UTILITY,
            "com.google.android.calculator" to UTILITY,
            "com.android.settings" to UTILITY,
            "com.google.android.apps.nbu.files" to UTILITY,
        )

        private fun guessFromPackage(pkg: String): AppCategory? =
            packageHints.firstOrNull { (prefix, _) -> pkg == prefix || pkg.startsWith("$prefix.") }?.second
    }
}
