package com.zeroproject.alice.device

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

/**
 * Explicit, user-initiated Android actions. No accessibility automation,
 * background control or privileged permissions are used here.
 */
class AliceDeviceActions(private val context: Context) {

    data class Action(val id: String, val title: String)

    fun availableActions(): List<Action> = listOf(
        Action("settings", "Открыть настройки телефона"),
        Action("wifi", "Открыть настройки Wi-Fi"),
        Action("bluetooth", "Открыть настройки Bluetooth"),
        Action("app_settings", "Разрешения ZERO: Alice"),
        Action("browser", "Открыть браузер")
    )

    fun execute(id: String): Boolean {
        val intent = when (id) {
            "settings" -> Intent(Settings.ACTION_SETTINGS)
            "wifi" -> Intent(Settings.ACTION_WIFI_SETTINGS)
            "bluetooth" -> Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
            "app_settings" -> Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:${context.packageName}")
            )
            "browser" -> Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com"))
            else -> return false
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }
}
