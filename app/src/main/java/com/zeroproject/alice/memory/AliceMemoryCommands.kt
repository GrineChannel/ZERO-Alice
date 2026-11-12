
package com.zeroproject.alice.memory

class AliceMemoryCommands(
    private val memory: AliceMemory
) {

    fun handle(text: String): String? {
        val message = text.trim()
        val lower = message.lowercase()

        if (
            lower == "что ты помнишь обо мне?" ||
            lower == "что ты помнишь?" ||
            lower == "покажи память" ||
            lower == "покажи, что ты помнишь" ||
            lower == "что ты знаешь обо мне?"
        ) {
            return showMemory()
        }

        if (
            lower.startsWith("забудь всё") ||
            lower.startsWith("забудь все") ||
            lower.startsWith("очисти память")
        ) {
            memory.clearFacts()
            memory.clearHistory()
            memory.clearForgottenTerms()
            return "Очистила сохранённые факты и историю переписки. ❤️"
        }

        if (
            lower.startsWith("забудь ") ||
            lower.startsWith("удали из памяти ")
        ) {
            return forgetFact(message)
        }

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

    private fun createFact(statement: String): Pair<String, String> {

        val patterns = listOf(
            Regex("^меня зовут\\s+(.+)$", RegexOption.IGNORE_CASE)
                to "Имя пользователя",

            Regex("^я живу в\\s+(.+)$", RegexOption.IGNORE_CASE)
                to "Место проживания",

            Regex("^мне\\s+(\\d+)\\s+лет$", RegexOption.IGNORE_CASE)
                to "Возраст пользователя"
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

        return "Факт: ${statement.lowercase().take(80)}" to statement
    }

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
            .trim(',', '.', ':', ';', '!', '?', '—', '-')
            .lowercase()

        if (query.isBlank()) {
            return "Уточни, что именно мне нужно забыть."
        }

        val aliases = mapOf(
            "где я живу" to "место проживания",
            "мой город" to "место проживания",
            "город" to "место проживания",
            "место проживания" to "место проживания",

            "мой возраст" to "возраст пользователя",
            "сколько мне лет" to "возраст пользователя",
            "возраст" to "возраст пользователя",

            "как меня зовут" to "имя пользователя",
            "моё имя" to "имя пользователя",
            "мое имя" to "имя пользователя",
            "имя" to "имя пользователя"
        )

        val searchTerm = aliases[query] ?: query

        val matches = memory.getFacts().filter { fact ->
            fact.key.contains(searchTerm, ignoreCase = true) ||
            fact.value.contains(searchTerm, ignoreCase = true)
        }

        if (matches.isEmpty()) {
            return "Я не нашла в памяти информацию по запросу: $query"
        }

        memory.addForgottenTerms(matches.map { it.value })
        matches.forEach { fact ->
            memory.deleteFact(fact.id)
        }

        return "Удалила факт из памяти и исключила его значение из будущего контекста. История чата пока сохранена."
    }
}
