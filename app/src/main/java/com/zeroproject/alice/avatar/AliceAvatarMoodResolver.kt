package com.zeroproject.alice.avatar
object AliceAvatarMoodResolver {
    fun forStatus(isGenerating: Boolean, isReady: Boolean): AliceEmotion =
        when {
            isGenerating -> AliceEmotion.THINKING
            isReady -> AliceEmotion.HAPPY
            else -> AliceEmotion.NEUTRAL
        }
}
