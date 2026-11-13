package com.zeroproject.alice.voice
object AliceVoiceModeLabels {
    fun label(mode: AliceVoiceMode): String = when (mode) {
        AliceVoiceMode.OFF -> "Выключен"
        AliceVoiceMode.PUSH_TO_TALK -> "Нажать для разговора"
        AliceVoiceMode.HANDS_FREE -> "Без рук (планируется)"
    }
}
