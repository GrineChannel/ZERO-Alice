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
