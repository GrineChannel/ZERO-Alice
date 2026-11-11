package com.zeroproject.alice.ai

import com.zeroproject.alice.AliceLocalEngine
import com.zeroproject.alice.AliceModelDownloader
import java.io.File

/**
 * Coordinates model availability checks and one-time engine initialization.
 * The Activity remains responsible for presenting the resulting UI messages.
 */
class AliceAIManager(
    private val modelFile: File,
    private val downloader: AliceModelDownloader,
    private val engine: AliceLocalEngine,
    val state: AliceAIState = AliceAIState()
) {
    enum class StartResult {
        READY,
        ALREADY_STARTING,
        MODEL_MISSING,
        STARTED
    }

    fun isReady(): Boolean = engine.isReady()

    fun start(
        onReady: () -> Unit,
        onError: (String) -> Unit
    ): StartResult {
        if (engine.isReady()) return StartResult.READY
        if (state.isInitializing || state.initializationStarted) {
            return StartResult.ALREADY_STARTING
        }
        if (!downloader.isModelDownloaded() || !modelFile.exists()) {
            return StartResult.MODEL_MISSING
        }
        if (!state.beginInitialization()) return StartResult.ALREADY_STARTING

        try {
            engine.initialize(
                onReady = {
                    state.initializationSucceeded()
                    onReady()
                },
                onError = { error ->
                    state.initializationFailed()
                    onError(error)
                }
            )
        } catch (error: Exception) {
            state.initializationFailed()
            onError(error.message ?: "Не удалось запустить локальную модель")
        }
        return StartResult.STARTED
    }
}
