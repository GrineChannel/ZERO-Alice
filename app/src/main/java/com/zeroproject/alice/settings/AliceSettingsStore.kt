package com.zeroproject.alice.settings
import android.content.Context
class AliceSettingsStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("alice_app_settings", Context.MODE_PRIVATE)
    fun load(): AliceAppSettings = AliceAppSettings(
        appearance = AliceAppearance.entries.firstOrNull { it.name == prefs.getString("appearance", null) } ?: AliceAppearance.DARK,
        accent = AliceAccent.entries.firstOrNull { it.name == prefs.getString("accent", null) } ?: AliceAccent.RED,
        voiceReplies = prefs.getBoolean("voice_replies", false),
        animatedAvatar = prefs.getBoolean("animated_avatar", true),
        saveChatHistory = prefs.getBoolean("save_chat_history", true)
    )
    fun save(settings: AliceAppSettings): Boolean = prefs.edit()
        .putString("appearance", settings.appearance.name)
        .putString("accent", settings.accent.name)
        .putBoolean("voice_replies", settings.voiceReplies)
        .putBoolean("animated_avatar", settings.animatedAvatar)
        .putBoolean("save_chat_history", settings.saveChatHistory).commit()
}
