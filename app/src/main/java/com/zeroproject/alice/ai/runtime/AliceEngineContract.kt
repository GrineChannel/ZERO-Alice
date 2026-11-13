package com.zeroproject.alice.ai.runtime

/**
 * Model-independent contract. Implementations can wrap LiteRTLM or a future ZERO AI.
 * Callbacks must be delivered on the Android main thread by the implementation.
 */
interface AliceEngineContract {
    val engineId: String
    fun isReady(): Boolean
    fun initialize(onReady: () -> Unit, onError: (String) -> Unit)
    fun generate(prompt: String, onResult: (String) -> Unit, onError: (String) -> Unit)
    fun close()
}
