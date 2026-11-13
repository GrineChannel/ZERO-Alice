package com.zeroproject.alice.voice
import android.content.Context
class AliceVoicePreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("alice_voice", Context.MODE_PRIVATE)
    fun mode(): AliceVoiceMode = AliceVoiceMode.entries.firstOrNull { it.name == prefs.getString("mode", null) } ?: AliceVoiceMode.PUSH_TO_TALK
    fun setMode(mode: AliceVoiceMode): Boolean = prefs.edit().putString("mode", mode.name).commit()
}
