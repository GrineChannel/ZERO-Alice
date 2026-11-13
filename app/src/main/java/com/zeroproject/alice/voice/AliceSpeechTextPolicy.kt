package com.zeroproject.alice.voice
object AliceSpeechTextPolicy {
    const val MAX_SPEECH_CHARACTERS = 3000
    fun prepare(text: String): String = text
        .replace(Regex("(?s)```.*?```"), " Фрагмент кода. ")
        .replace(Regex("[#*_]"), "")
        .trim()
        .take(MAX_SPEECH_CHARACTERS)
}
