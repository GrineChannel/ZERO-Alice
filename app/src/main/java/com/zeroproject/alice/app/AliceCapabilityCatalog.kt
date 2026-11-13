package com.zeroproject.alice.app
object AliceCapabilityCatalog {
    fun baseline(): List<AliceCapability> = AliceFeature.entries.map { feature ->
        val status = when (feature) {
            AliceFeature.MEMORY, AliceFeature.FILES, AliceFeature.TASKS -> AliceCapabilityStatus.READY
            AliceFeature.AVATAR, AliceFeature.MUSIC, AliceFeature.SETTINGS -> AliceCapabilityStatus.PLANNED
            else -> AliceCapabilityStatus.NEEDS_SETUP
        }
        AliceCapability(feature, status, "Базовый статус; фактическую доступность проверяет соответствующий модуль")
    }
}
