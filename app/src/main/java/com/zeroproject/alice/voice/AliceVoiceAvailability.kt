package com.zeroproject.alice.voice
data class AliceVoiceAvailability(val inputEnabled: Boolean, val outputEnabled: Boolean)
object AliceVoiceAvailabilityResolver {
    fun resolve(mode: AliceVoiceMode, ttsReady: Boolean): AliceVoiceAvailability =
        AliceVoiceAvailability(mode != AliceVoiceMode.OFF, ttsReady)
}
