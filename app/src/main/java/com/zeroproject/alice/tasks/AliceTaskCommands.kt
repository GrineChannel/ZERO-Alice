package com.zeroproject.alice.tasks

/**
 * Explicit commands for a task plan. Marking a step done records progress;
 * it does not execute the step or grant permission to perform actions.
 */
class AliceTaskCommands(private val store: AliceTaskStore) {

    fun handle(input: String): String? {
        val text = input.trim()
        val lower = text.lowercase()

        if (lower == "покажи задачи элис" || lower == "список задач элис") {
            val tasks = store.list()
            if (tasks.isEmpty()) return "Задач пока нет."
            return tasks.takeLast(20).joinToString("\n") {
                "${it.id.take(8)} | ${it.status} | ${it.completedSteps}/${it.steps.size} | ${it.goal.take(80)}"
            }
        }

        if (lower.startsWith("создай задачу элис ")) {
            val specification = text.substring("создай задачу элис ".length)
            val parts = specification.split(" :: ").map { it.trim() }
            if (parts.size < 2 || parts.any { it.isBlank() }) {
                return "Формат: создай задачу элис Цель :: Шаг 1 :: Шаг 2"
            }
            return try {
                val task = store.create(parts.first(), parts.drop(1))
                "План создан: ${task.id.take(8)}. Шагов: ${task.steps.size}. Выполнение пока не запущено."
            } catch (error: Exception) {
                "Не удалось создать задачу: ${error.message}"
            }
        }

        if (lower.startsWith("шаг задачи элис ")) {
            val prefix = text.substring("шаг задачи элис ".length).trim()
            return update(prefix, advance = true)
        }

        if (lower.startsWith("отмени задачу элис ")) {
            val prefix = text.substring("отмени задачу элис ".length).trim()
            return update(prefix, advance = false)
        }

        return null
    }

    private fun update(prefix: String, advance: Boolean): String {
        if (prefix.length < 4) return "Укажи идентификатор задачи (не менее 4 символов)."
        val matches = store.list().filter { it.id.startsWith(prefix, ignoreCase = true) }
        if (matches.size != 1) return "Задача не найдена или идентификатор неоднозначен."
        val task = if (advance) store.advance(matches.first().id) else store.cancel(matches.first().id)
        return if (task == null) "Задача не найдена."
        else "Задача ${task.id.take(8)}: ${task.status}, ${task.completedSteps}/${task.steps.size} шагов."
    }
}
