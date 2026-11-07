
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
            return "Хорошо. Я очистила свою долговременную память."
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

        val key = "Факт: " +
            statement.lowercase().take(80)

        return key to statement
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
            .removePrefix("Забудь ")
            .removePrefix("забудь ")
            .removePrefix("Удали из памяти ")
            .removePrefix("удали из памяти ")
            .trim()
            .trimEnd('.', '!', '?')
            .lowercase()

        if (query.isBlank()) {
            return "Уточни, что именно мне нужно забыть."
        }

        val matches = memory.getFacts().filter {
            it.key.lowercase().contains(query) ||
            it.value.lowercase().contains(query)
        }

        if (matches.isEmpty()) {
            return "Я не нашла в памяти информацию по запросу: $query"
        }

        matches.forEach {
            memory.deleteFact(it.id)
        }

        return if (matches.size == 1) {
            "Хорошо. Я забыла эту информацию."
        } else {
            "Хорошо. Я удалила ${matches.size} записей из памяти."
        }
    }
}
