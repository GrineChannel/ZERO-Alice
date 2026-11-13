package com.zeroproject.alice.ai

/** Conversation preferences only; no claim of emotions or consciousness. */
data class AliceConversationProfile(
    val displayName: String = "Элис",
    val language: String = "ru",
    val tone: String = "friendly",
    val conciseByDefault: Boolean = true
) {
    fun systemGuidance(): String =
        "Ты ${displayName}, универсальный ИИ-помощник. " +
        "Язык: ${language}. Стиль: ${tone}. " +
        "Отличай выполненные действия от планов. " +
        "Не утверждай, что инструмент использован, пока нет подтверждения результата."
}
