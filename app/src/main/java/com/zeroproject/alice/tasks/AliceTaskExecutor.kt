package com.zeroproject.alice.tasks

import com.zeroproject.alice.files.AliceWorkspace

/**
 * Executes exactly one explicitly requested task step.
 * Only private workspace operations are supported. Other steps remain pending.
 */
class AliceTaskExecutor(
    private val store: AliceTaskStore,
    private val workspace: AliceWorkspace
) {
    /**
     * Runs at most five steps per explicit user request. A failed or unsupported
     * step stops the batch without marking it complete. A later request resumes
     * from the last persisted completed step.
     */
    fun executeBatch(taskId: String, maxSteps: Int = 5): String {
        val limit = maxSteps.coerceIn(1, 5)
        val messages = mutableListOf<String>()
        for (index in 0 until limit) {
            val before = store.list().firstOrNull { it.id == taskId }
                ?: return "Задача не найдена."
            if (before.status == "completed" || before.status == "cancelled") break
            val result = executeNext(taskId)
            messages.add(result)
            val after = store.list().firstOrNull { it.id == taskId }
                ?: break
            if (after.completedSteps <= before.completedSteps) break
            if (after.status == "completed" || after.status == "cancelled") break
        }
        return if (messages.isEmpty()) {
            "Задача завершена или отменена."
        } else {
            messages.joinToString("\\n")
        }
    }

    fun executeNext(taskId: String): String {
        val task = store.list().firstOrNull { it.id == taskId }
            ?: return "Задача не найдена."
        if (task.status == "cancelled" || task.status == "completed") {
            return "Задача уже завершена или отменена."
        }
        val step = task.steps.getOrNull(task.completedSteps)
            ?: return "Все шаги уже отмечены."
        val lower = step.lowercase().trim()
        return try {
            val result = when {
                lower.startsWith("создай папку элис ") -> {
                    val path = step.substring("создай папку элис ".length).trim()
                    if (!workspace.createDirectory(path)) {
                        return "Не удалось создать папку. Шаг не завершён."
                    }
                    "Создана папка: $path"
                }
                lower.startsWith("создай файл элис ") -> {
                    val args = step.substring("создай файл элис ".length)
                    val separator = args.indexOf(" :: ")
                    if (separator <= 0) {
                        return "Неверный формат шага. Используй: создай файл элис имя.txt :: содержимое"
                    }
                    val path = args.substring(0, separator).trim()
                    val content = args.substring(separator + 4)
                    workspace.writeText(path, content, overwrite = false)
                    "Создан файл: $path"
                }
                else -> return "Этот шаг пока не поддерживается исполнителем: $step"
            }
            val updated = store.advance(task.id)
                ?: return "$result. Но не удалось сохранить прогресс."
            "$result. Прогресс: ${updated.completedSteps}/${updated.steps.size}."
        } catch (error: Exception) {
            "Шаг не выполнен: ${error.message}"
        }
    }
}
