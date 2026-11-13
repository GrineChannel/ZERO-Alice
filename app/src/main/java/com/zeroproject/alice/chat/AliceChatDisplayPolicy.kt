package com.zeroproject.alice.chat
object AliceChatDisplayPolicy {
    const val MAX_DISPLAY_CHARACTERS = 120_000
    fun safePreview(text: String): String = if (text.length <= MAX_DISPLAY_CHARACTERS) text else text.take(MAX_DISPLAY_CHARACTERS) + "\n…"
}
