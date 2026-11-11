package com.zeroproject.alice.ai

/**
 * UI-facing state for the shared local AI engine.
 *
 * This class does not initialize LiteRTLM itself; it keeps the lifecycle
 * flags consistent while the existing engine continues to own inference.
 */
class AliceAIState {

    var isInitializing: Boolean = false
        private set

    var initializationStarted: Boolean = false
        private set

    var isGenerating: Boolean = false
        private set

    fun beginInitialization(): Boolean {
        if (isInitializing || initializationStarted) return false
        isInitializing = true
        initializationStarted = true
        return true
    }

    fun initializationSucceeded() {
        isInitializing = false
        initializationStarted = true
    }

    fun initializationFailed() {
        isInitializing = false
        initializationStarted = false
    }

    fun beginGeneration(): Boolean {
        if (isGenerating) return false
        isGenerating = true
        return true
    }

    fun finishGeneration() {
        isGenerating = false
    }

    fun statusText(engineReady: Boolean): String = when {
        isGenerating -> "●  ЭЛИС ДУМАЕТ..."
        engineReady -> "●  ЛОКАЛЬНЫЙ ИИ АКТИВЕН"
        isInitializing -> "●  ЗАГРУЗКА ЛОКАЛЬНОГО ИИ..."
        else -> "●  ОЖИДАНИЕ ИИ"
    }
}
