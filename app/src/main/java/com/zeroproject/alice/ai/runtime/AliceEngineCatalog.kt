package com.zeroproject.alice.ai.runtime

enum class AliceEngineKind { LOCAL, REMOTE }

data class AliceEngineDescriptor(
    val id: String,
    val title: String,
    val kind: AliceEngineKind,
    val description: String,
    val available: Boolean
)

object AliceEngineCatalog {
    const val GEMMA = "gemma_litertlm"
    const val ZERO_AI = "zero_ai"

    fun entries(): List<AliceEngineDescriptor> = listOf(
        AliceEngineDescriptor(
            GEMMA, "Gemma (LiteRTLM)", AliceEngineKind.LOCAL,
            "Временная локальная модель для тестирования приложения", true
        ),
        AliceEngineDescriptor(
            ZERO_AI, "ZERO AI", AliceEngineKind.LOCAL,
            "Собственный движок Элис: подключение запланировано", false
        )
    )

    fun find(id: String): AliceEngineDescriptor? = entries().firstOrNull { it.id == id }
}
