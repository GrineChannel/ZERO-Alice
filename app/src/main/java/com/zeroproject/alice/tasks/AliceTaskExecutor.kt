package com.zeroproject.alice.tasks

import com.zeroproject.alice.files.AliceWorkspace
import com.zeroproject.alice.files.AliceFileRevision
import com.zeroproject.alice.engineer.minecraft.MinecraftProjectManager

/**
 * Executes exactly one explicitly requested task step.
 * Only private workspace operations are supported. Other steps remain pending.
 */
class AliceTaskExecutor(
    private val store: AliceTaskStore,
    private val workspace: AliceWorkspace,
    private val log: AliceTaskLog? = null,
    private val revisions: AliceFileRevision? = null,
    private val minecraftProjects: MinecraftProjectManager? = null,
    private val launchMinecraft: ((String, String, (Boolean, String) -> Unit) -> Boolean)? = null
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
            // Batch execution never overwrites existing files or performs undo.
            val pending = before.steps.getOrNull(before.completedSteps)?.trim()?.lowercase().orEmpty()
            if (pending.startsWith("замени файл элис ") || pending.startsWith("откати файл элис ") || pending.startsWith("разработай minecraft элис ")) {
                messages.add("Этот шаг требует отдельной команды «выполни шаг элис ID».")
                break
            }
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
            messages.joinToString("\n")
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
        if (lower.startsWith("разработай minecraft элис ")) {
            val args = step.substring("разработай minecraft элис ".length)
            val separator = args.indexOf(" :: ")
            if (separator < 4) return "Формат шага: разработай minecraft элис ID :: цель"
            val projectId = args.substring(0, separator).trim()
            val goal = args.substring(separator + 4).trim()
            if (goal.isBlank()) return "Цель разработки не указана."
            val launcher = launchMinecraft ?: return "Запуск Minecraft-разработки недоступен."
            val accepted = launcher(projectId, goal) { success, details ->
                try {
                    if (success) {
                        val current = store.list().firstOrNull { it.id == task.id }
                        if (current != null && current.status != "cancelled" &&
                            current.completedSteps == task.completedSteps) {
                            store.advance(task.id)
                        }
                    }
                    log?.append(task.id, task.completedSteps, success, details)
                } catch (_: Exception) {
                    // Keep the engineering callback from crashing the UI.
                }
            }
            return if (accepted) "Разработка Minecraft запущена. Прогресс задачи обновится после результата."
            else "Запуск Minecraft-разработки отклонён. Шаг не завершён."
        }
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
                lower.startsWith("создай minecraft проект элис ") -> {
                    val name = step.substring("создай minecraft проект элис ".length).trim()
                    require(name.isNotBlank()) { "Название Minecraft-проекта не указано" }
                    val manager = minecraftProjects ?: return "Модуль Minecraft-проектов недоступен."
                    val project = manager.createProject(name)
                    "Создан Minecraft Bedrock проект: ${project.name} (ID: ${project.id})"
                }
                lower.startsWith("предпросмотр замены элис ") ||
                    lower.startsWith("замени файл элис ") -> {
                    val preview = lower.startsWith("предпросмотр замены элис ")
                    val prefix = if (preview) "предпросмотр замены элис " else "замени файл элис "
                    val args = step.substring(prefix.length)
                    val separator = args.indexOf(" :: ")
                    require(separator > 0) { "Укажи путь и содержимое через :: " }
                    val path = args.substring(0, separator).trim()
                    val content = args.substring(separator + 4)
                    val editor = revisions ?: return "Редактор файлов недоступен."
                    if (preview) editor.preview(path, content)
                    else editor.replace(path, content)
                }
                lower.startsWith("откати файл элис ") -> {
                    val editor = revisions ?: return "Редактор файлов недоступен."
                    editor.undo(step.substring("откати файл элис ".length).trim())
                }
                else -> {
                    val message = "Этот шаг пока не поддерживается исполнителем: $step"
                    log?.append(task.id, task.completedSteps, false, message)
                    return message
                }
            }
            val updated = store.advance(task.id)
                ?: return "$result. Но не удалось сохранить прогресс."
            log?.append(task.id, task.completedSteps, true, result)
            "$result. Прогресс: ${updated.completedSteps}/${updated.steps.size}."
        } catch (error: Exception) {
            val message = "Шаг не выполнен: ${error.message}"
            log?.append(task.id, task.completedSteps, false, message)
            message
        }
    }
}
