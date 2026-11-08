
package com.zeroproject.alice.memory

import com.zeroproject.alice.AliceLocalEngine

class AliceMemoryManager(
    private val memory: AliceMemory,
    private val engine: AliceLocalEngine
) {

    private val extractor = AliceMemoryExtractor()

    @Volatile
    private var analysisRunning = false

    /**
     * Передаёт диалог локальной Gemma
     * для извлечения важных воспоминаний.
     */
    fun analyzeDialogue(
        dialogue: String,
        onComplete: (Int) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {

        if (dialogue.isBlank()) {
            onComplete(0)
            return
        }

        synchronized(this) {
            if (analysisRunning) {
                onComplete(0)
                return
            }

            analysisRunning = true
        }

        val prompt = extractor.buildPrompt(dialogue)

        engine.analyzeMemory(
            prompt = prompt,

            onResult = { response ->

                try {
                    val extracted = extractor.parseResponse(response)

                    var savedCount = 0

                    for (fact in extracted) {

                        if (containsSensitiveInformation(
                                fact.key + " " + fact.value
                            )
                        ) {
                            continue
                        }

                        if (saveOrUpdateFact(fact)) {
                            savedCount++
                        }
                    }

                    onComplete(savedCount)

                } catch (error: Throwable) {

                    onError(
                        error.message ?: "Ошибка сохранения памяти"
                    )

                } finally {

                    synchronized(this) {
                        analysisRunning = false
                    }
                }
            },

            onError = { error ->

                synchronized(this) {
                    analysisRunning = false
                }

                onError(error)
            }
        )
    }

    /**
     * Сохраняет новый факт или обновляет существующий.
     */
    private fun saveOrUpdateFact(
        fact: ExtractedMemory
    ): Boolean {

        val key = fact.key.trim()
        val value = fact.value.trim()

        if (key.isBlank() || value.isBlank()) {
            return false
        }

        if (key.length > 100 || value.length > 500) {
            return false
        }

        val existingFacts = memory.getFacts()

        val existing = existingFacts.firstOrNull {
            it.key.equals(key, ignoreCase = true)
        }

        if (existing != null) {

            if (existing.value.equals(value, ignoreCase = true)) {
                return false
            }

            memory.saveFact(key, value)
            return true
        }

        if (existingFacts.size >= 500) {
            return false
        }

        memory.saveFact(key, value)
        return true
    }

    /**
     * Фильтрация чувствительной информации.
     */
    private fun containsSensitiveInformation(
        text: String
    ): Boolean {

        val patterns = listOf(
            Regex("""(?i)\bpassword\b"""),
            Regex("""(?i)\bпароль\b"""),
            Regex("""(?i)\btoken\b"""),
            Regex("""(?i)\bapi[_ -]?key\b"""),
            Regex("""(?i)\bsecret\b"""),
            Regex("""(?i)\bprivate[_ -]?key\b"""),
            Regex("""(?i)\bключ доступа\b"""),
            Regex("""(?i)\bномер карты\b"""),
            Regex("""(?i)\bcvv\b""")
        )

        return patterns.any {
            it.containsMatchIn(text)
        }
    }

    /**
     * Получение воспоминаний.
     */
    fun getMemories(): List<AliceFact> {
        return memory.getFacts()
    }

    /**
     * Удаление воспоминания.
     */
    fun forget(id: Long) {
        memory.deleteFact(id)
    }

    /**
     * Полная очистка долговременной памяти.
     */
    fun clear() {
        memory.clearFacts()
    }

    fun isAnalyzing(): Boolean {
        return analysisRunning
    }
}
