package com.zeroproject.alice.app
import android.content.Context
object AliceAppVersion {
    fun label(context: Context): String = try {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        info.versionName ?: "неизвестно"
    } catch (_: Exception) {
        "неизвестно"
    }
}
