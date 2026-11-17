package com.zeroproject.alice.app

import android.content.Context
import android.os.Build

object AliceAppVersion {
    fun label(context: Context): String = try {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        val versionName = info.versionName ?: "неизвестно"
        val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            info.versionCode.toLong()
        }
        "$versionName (сборка $code)"
    } catch (_: Exception) {
        "неизвестно"
    }
}
