package com.zeroproject.alice.ai.runtime

import com.zeroproject.alice.AliceLocalEngine

/** Adapter between the current LiteRTLM engine and the future model-independent runtime. */
class AliceGemmaEngineAdapter(private val delegate: AliceLocalEngine) : AliceConversationEngine {
    override val engineId: String = AliceEngineCatalog.GEMMA

    override fun isReady(): Boolean = delegate.isReady()

    override fun initialize(onReady: () -> Unit, onError: (String) -> Unit) =
        delegate.initialize(onReady, onError)

    override fun generate(
        prompt: String,
        onResult: (String) -> Unit,
        onError: (String) -> Unit
    ) = delegate.sendMessage(prompt, onResult, onError)

    override fun resetConversation(onComplete: () -> Unit, onError: (String) -> Unit) =
        delegate.resetChatConversation(onComplete, onError)

    override fun analyzeMemory(prompt: String, onResult: (String) -> Unit, onError: (String) -> Unit) =
        delegate.analyzeMemory(prompt, onResult, onError)

    override fun close() = delegate.close()
}
