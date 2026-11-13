package com.zeroproject.alice.ai.runtime

/**
 * Keeps engine ownership outside screens and activities.
 * A registered engine is not automatically initialized.
 */
class AliceEngineRegistry {
    private val engines = linkedMapOf<String, AliceEngineContract>()

    @Synchronized
    fun register(engine: AliceEngineContract) {
        require(AliceEngineCatalog.find(engine.engineId) != null) {
            "Unknown engine: ${engine.engineId}"
        }
        engines[engine.engineId] = engine
    }

    @Synchronized
    fun resolve(id: String): AliceEngineContract? = engines[id]

    @Synchronized
    fun registeredIds(): List<String> = engines.keys.toList()

    @Synchronized
    fun unregister(id: String): AliceEngineContract? = engines.remove(id)
}
