package com.zeroproject.alice.tasks

/**
 * Explicit commands for a task plan. Marking a step done records progress;
 * it does not execute the step or grant permission to perform actions.
 */
class AliceTaskCommands(
    private val store: AliceTaskStore,
    private val executor: AliceTaskExecutor? = null,
    private val log: AliceTaskLog? = null
) {

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

        if (lower.startsWith("создай minecraft задачу элис ")) {
            val specification = text.substring("создай minecraft задачу элис ".length)
            val separator = specification.indexOf(" :: ")
            if (separator < 4) {
                return "Формат: создай minecraft задачу элис ID :: цель разработки"
            }
            val projectId = specification.substring(0, separator).trim()
            val goal = specification.substring(separator + 4).trim()
            if (goal.isBlank()) return "Укажи цель разработки Minecraft."
            return try {
                val task = store.create(
                    "Minecraft Bedrock: $goal",
                    listOf("разработай minecraft элис $projectId :: $goal")
                )
                "План Minecraft создан: ${task.id.take(8)}. Для запуска используй «выполни шаг элис ${task.id.take(8)}»."
            } catch (error: Exception) {
                "Не удалось создать Minecraft-план: ${error.message}"
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

        if (lower.startsWith("предложи исправления элис ")) {
            val prefix = text.substring("предложи исправления элис ".length).trim()
            if (prefix.length < 4) return "Укажи идентификатор задачи (не менее 4 символов)."
            val matches = store.list().filter { it.id.startsWith(prefix, ignoreCase = true) }
            if (matches.size != 1) return "Задача не найдена или идентификатор неоднозначен."
            return log?.let {
                AliceRepairPlanner(AliceTaskDiagnostics(it)).report(matches.first().id)
            } ?: "Журнал задач пока недоступен."
        }

        if (lower.startsWith("диагностика задачи элис ")) {
            val prefix = text.substring("диагностика задачи элис ".length).trim()
            if (prefix.length < 4) return "Укажи идентификатор задачи (не менее 4 символов)."
            val matches = store.list().filter { it.id.startsWith(prefix, ignoreCase = true) }
            if (matches.size != 1) return "Задача не найдена или идентификатор неоднозначен."
            return log?.let { AliceTaskDiagnostics(it).report(matches.first().id) }
                ?: "Журнал задач пока недоступен."
        }

        if (lower.startsWith("журнал задачи элис ")) {
            val prefix = text.substring("журнал задачи элис ".length).trim()
            if (prefix.length < 4) return "Укажи идентификатор задачи (не менее 4 символов)."
            val matches = store.list().filter { it.id.startsWith(prefix, ignoreCase = true) }
            if (matches.size != 1) return "Задача не найдена или идентификатор неоднозначен."
            val records = log?.recent(matches.first().id).orEmpty()
            return if (records.isEmpty()) "Записей выполнения пока нет."
            else records.joinToString("\n") {
                "${if (it.success) "OK" else "ERROR"} | шаг ${it.stepIndex + 1} | ${it.message}"
            }
        }

        if (lower.startsWith("продолжи задачу элис ")) {
            val prefix = text.substring("продолжи задачу элис ".length).trim()
            if (prefix.length < 4) return "Укажи идентификатор задачи (не менее 4 символов)."
            val matches = store.list().filter { it.id.startsWith(prefix, ignoreCase = true) }
            if (matches.size != 1) return "Задача не найдена или идентификатор неоднозначен."
            return executor?.executeBatch(matches.first().id)
                ?: "Исполнитель задач пока недоступен."
        }

        if (lower.startsWith("выполни шаг элис ")) {
            val prefix = text.substring("выполни шаг элис ".length).trim()
            if (prefix.length < 4) return "Укажи идентификатор задачи (не менее 4 символов)."
            val matches = store.list().filter { it.id.startsWith(prefix, ignoreCase = true) }
            if (matches.size != 1) return "Задача не найдена или идентификатор неоднозначен."
            return executor?.executeNext(matches.first().id)
                ?: "Исполнитель задач пока недоступен."
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
