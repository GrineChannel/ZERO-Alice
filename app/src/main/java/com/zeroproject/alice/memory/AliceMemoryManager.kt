
package com.zeroproject.alice.memory

class AliceMemoryManager(
    private val memory: AliceMemory
) {

    companion object {
        private const val MAX_FACT_LENGTH = 500
        private const val MAX_FACTS = 500

        private val sensitivePatterns = listOf(
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
    }

    /**
     * Анализирует сообщение и сохраняет
     * полезную информацию без подтверждения.
     */
    fun analyzeAndSave(text: String): Int {

        if (text.isBlank()) return 0

        val normalized = text.trim()

        if (normalized.length > MAX_FACT_LENGTH) {
            return 0
        }

        if (containsSensitiveInformation(normalized)) {
            return 0
        }

        val fact = extractFact(normalized)
            ?: return 0

        val existingFacts = memory.getFacts()

        if (
            existingFacts.none {
                it.key == fact.first &&
                it.value.equals(fact.second, ignoreCase = true)
            }
        ) {
            if (existingFacts.size >= MAX_FACTS) {
                return 0
            }

            memory.saveFact(
                fact.first,
                fact.second
            )

            return 1
        }

        return 0
    }

    /**
     * Определяет простые факты
     * по формулировкам пользователя.
     */
    private fun extractFact(text: String): Pair<String, String>? {

        val patterns = listOf(
            Triple(
                Regex("""(?i)^меня зовут\s+(.+)$"""),
                "имя пользователя",
                1
            ),
            Triple(
                Regex("""(?i)^моё имя\s+(.+)$"""),
                "имя пользователя",
                1
            ),
            Triple(
                Regex("""(?i)^мне нравится\s+(.+)$"""),
                "предпочтения пользователя",
                1
            ),
            Triple(
                Regex("""(?i)^я люблю\s+(.+)$"""),
                "интересы пользователя",
                1
            ),
            Triple(
                Regex("""(?i)^мой проект называется\s+(.+)$"""),
                "название проекта",
                1
            ),
            Triple(
                Regex("""(?i)^я работаю над проектом\s+(.+)$"""),
                "текущий проект",
                1
            ),
            Triple(
                Regex("""(?i)^я живу в\s+(.+)$"""),
                "место проживания",
                1
            ),
            Triple(
                Regex("""(?i)^моя цель\s*[:—-]?\s*(.+)$"""),
                "цель пользователя",
                1
            ),
            Triple(
                Regex("""(?i)^я планирую\s+(.+)$"""),
                "планы пользователя",
                1
            )
        )

        for ((regex, key, group) in patterns) {

            val match = regex.find(text)
                ?: continue

            val value = match.groupValues[group]
                .trim()
                .trimEnd('.', '!', '?')

            if (value.isBlank()) return null

            return key to value
        }

        return null
    }

    /**
     * Защита от сохранения секретов.
     */
    private fun containsSensitiveInformation(
        text: String
    ): Boolean {

        return sensitivePatterns.any {
            it.containsMatchIn(text)
        }
    }

    /**
     * Получение текущей долговременной памяти.
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
}
