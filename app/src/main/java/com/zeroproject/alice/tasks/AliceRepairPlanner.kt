package com.zeroproject.alice.tasks

/**
 * Read-only repair planning from deterministic diagnostics.
 * Suggestions do not modify files or automatically retry tasks.
 */
class AliceRepairPlanner(
    private val diagnostics: AliceTaskDiagnostics
) {
    data class Proposal(
        val stepIndex: Int,
        val issue: String,
        val proposedAction: String,
        val requiresReview: Boolean = true
    )

    fun proposals(taskId: String): List<Proposal> =
        diagnostics.analyze(taskId).map { finding ->
            val action = when (finding.category) {
                "file_exists" ->
                    "Сравнить существующий файл с ожидаемым содержимым; затем вручную выбрать новое имя или разрешить перезапись."
                "unsafe_path" ->
                    "Заменить путь на относительный внутри рабочего пространства Элис."
                "file_size" ->
                    "Разделить большой файл на несколько файлов меньшего размера."
                "unsupported" ->
                    "Переписать шаг в поддерживаемую файловую команду либо добавить новый инструмент."
                "progress" ->
                    "Сверить состояние файлов и прогресс до повторного выполнения."
                else ->
                    "Изучить журнал и проверить исходный шаг перед изменениями."
            }
            Proposal(finding.stepIndex, finding.explanation, action)
        }

    fun report(taskId: String): String {
        val items = proposals(taskId)
        if (items.isEmpty()) return "Ошибок, для которых нужны предложения исправлений, не найдено."
        return items.takeLast(10).mapIndexed { index, item ->
            "${index + 1}. Шаг ${item.stepIndex + 1}: ${item.issue}\nПредложение: ${item.proposedAction}\nСтатус: требуется проверка."
        }.joinToString("\n\n")
    }
}
