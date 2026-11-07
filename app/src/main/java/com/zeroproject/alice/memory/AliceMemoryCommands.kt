
package com.zeroproject.alice.memory

class AliceMemoryCommands(
    private val memory: AliceMemory
) {

    fun handle(text: String): String? {
        val message = text.trim()
        val lower = message.lowercase()

        // Просмотр долговременной памяти.

        if (
            lower == "что ты помнишь обо мне?" ||
            lower == "что ты помнишь?" ||
            lower == "покажи память" ||
            lower == "покажи, что ты помнишь" ||
            lower == "что ты знаешь обо мне?"
        ) {
            return showMemory()
        }

        // Полная очистка памяти.

        if (
            lower.startsWith("забудь всё") ||
            lower.startsWith("забудь все") ||
            lower.startsWith("очисти память")
        ) {
            memory.clearFacts()
            return "Хорошо. Я очистила свою долговременную память. ❤️"
        }

        // Удаление отдельных фактов.

        if (
            lower.startsWith("забудь ") ||
            lower.startsWith("удали из памяти ")
        ) {
            return forgetFact(message)
        }

        // Сохранение новых фактов.

        val prefixes = listOf(
            "запомни, что ",
            "запомни что ",
            "сохрани, что ",
            "сохрани что ",
            "запомни: ",
            "сохрани: "
        )

        val prefix = prefixes.firstOrNull {
            lower.startsWith(it)
        } ?: return null

        val statement = message
            .substring(prefix.length)
            .trim()
            .trimEnd('.', '!', '?')

        if (statement.isBlank()) {
            return "Что именно мне нужно запомнить?"
        }

        val (key, value) = createFact(statement)

        val result = memory.saveFact(key, value)

        return if (result != -1L) {
            "Запомнила! ❤️\n\n$key: $value"
        } else {
            "Не получилось сохранить эту информацию."
        }
    }

    // Определение категории факта.

    private fun createFact(statement: String): Pair<String, String> {

        val patterns = listOf(
            Regex(
                "^меня зовут\\s+(.+)$",
                RegexOption.IGNORE_CASE
            ) to "Имя пользователя",

            Regex(
                "^я живу в\\s+(.+)$",
                RegexOption.IGNORE_CASE
            ) to "Место проживания",

            Regex(
                "^мне\\s+(\\d+)\\s+лет$",
                RegexOption.IGNORE_CASE
            ) to "Возраст пользователя"
        )

        for ((pattern, key) in patterns) {
            val match = pattern.find(statement)

            if (match != null) {
                return key to match.groupValues[1].trim()
            }
        }

        val myFact = Regex(
            "^мой\\s+(.+?)\\s+(?:это|—|-)\\s+(.+)$",
            RegexOption.IGNORE_CASE
        ).find(statement)

        if (myFact != null) {
            return "Мой ${myFact.groupValues[1].trim()}" to
                myFact.groupValues[2].trim()
        }

        val key = "Факт: " +
            statement.lowercase().take(80)

        return key to statement
    }

    // Просмотр сохранённых фактов.

    private fun showMemory(): String {
        val facts = memory.getFacts()

        if (facts.isEmpty()) {
            return "Пока моя долговременная память пуста. " +
                "Можешь попросить меня что-нибудь запомнить."
        }

        val result = facts.joinToString("\n\n") {
            "• ${it.key}: ${it.value}"
        }

        return "Вот что хранится в моей памяти:\n\n$result"
    }

    // Удаление фактов с учётом альтернативных формулировок.

    private fun forgetFact(message: String): String {

        val query = message
            .replaceFirst(
                Regex("^забудь\\s+", RegexOption.IGNORE_CASE),
                ""
            )
            .replaceFirst(
                Regex("^удали из памяти\\s+", RegexOption.IGNORE_CASE),
                ""
            )
            .trim()
            .trimEnd('.', '!', '?')
            .lowercase()

        if (query.isBlank()) {
            return "Уточни, что именно мне нужно забыть."
        }

        // Альтернативные названия категорий.

        val aliases = mapOf(
            "где я живу" to listOf("место проживания"),
            "мой город" to listOf("место проживания"),
            "город" to listOf("место проживания"),
            "место проживания" to listOf("место проживания"),

            "мой возраст" to listOf("возраст пользователя"),
            "сколько мне лет" to listOf("возраст пользователя"),
            "возраст" to listOf("возраст пользователя"),

            "как меня зовут" to listOf("имя пользователя"),
            "моё имя" to listOf("имя пользователя"),
            "мое имя" to listOf("имя пользователя"),
            "имя" to listOf("имя пользователя")
        )

        val searchTerms = aliases[query] ?: listOf(query)

        val matches = memory.getFacts().filter { fact ->

            searchTerms.any { term ->
                fact.key.lowercase().contains(term) ||
                fact.value.lowercase().contains(term)
            }
        }

        if (matches.isEmpty()) {
            return "Я не нашла в памяти информацию по запросу: $query"
        }

        matches.forEach { fact ->
            memory.deleteFact(fact.id)
        }

        return if (matches.size == 1) {
            "Хорошо. Я забыла эту информацию. ❤️"
        } else {
            "Хорошо. Я удалила ${matches.size} записей из памяти. ❤️"
        }
    }
}
