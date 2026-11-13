package com.zeroproject.alice.ai.runtime

/** Conversation capabilities used by chat and memory independently of a model SDK. */
interface AliceConversationEngine : AliceEngineContract {
    fun resetConversation(onComplete: () -> Unit, onError: (String) -> Unit)
    fun analyzeMemory(prompt: String, onResult: (String) -> Unit, onError: (String) -> Unit)
}
