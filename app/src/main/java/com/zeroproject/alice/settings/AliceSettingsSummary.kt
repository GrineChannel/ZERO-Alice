package com.zeroproject.alice.settings
object AliceSettingsSummary {
    fun describe(settings: AliceAppSettings): String =
        "Тема: ${settings.appearance.name}, акцент: ${settings.accent.name}, " +
        "голосовые ответы: ${if (settings.voiceReplies) "вкл" else "выкл"}"
}
