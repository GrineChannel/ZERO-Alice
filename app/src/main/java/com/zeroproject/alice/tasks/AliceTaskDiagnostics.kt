package com.zeroproject.alice.tasks

/**
 * Deterministic, read-only diagnosis of task execution failures.
 * Suggestions are informational: no file is changed automatically.
 */
class AliceTaskDiagnostics(private val log: AliceTaskLog) {
    data class Finding(
        val stepIndex: Int,
        val category: String,
        val explanation: String,
        val suggestion: String
    )

    fun analyze(taskId: String): List<Finding> {
        return log.recent(taskId, 50)
            .filterNot { it.success }
            .takeLast(10)
            .map { record ->
                val message = record.message.lowercase()
                val finding = when {
                    "уже существует" in message ->
                        Triple("file_exists", "Файл уже существует.",
                            "Проверь содержимое файла перед повторным созданием. Перезапись выполняется только отдельным разрешённым действием.")
                    "выходит за пределы" in message || "относительный путь" in message ->
                        Triple("unsafe_path", "Путь не соответствует границам рабочего пространства.",
                            "Используй относительный путь внутри папки Элис, например project/src/main.js.")
                    "слишком большой" in message || "лимит размера" in message ->
                        Triple("file_size", "Превышен допустимый размер файла.",
                            "Раздели содержимое на несколько небольших файлов.")
                    "не поддерживается" in message ->
                        Triple("unsupported", "Исполнитель не поддерживает этот тип шага.",
                            "Измени план на поддерживаемые команды создания папок и файлов.")
                    "не удалось сохранить прогресс" in message ->
                        Triple("progress", "Операция могла выполниться, но прогресс не сохранился.",
                            "Проверь созданные файлы и журнал перед повторным запуском, чтобы избежать дублирования.")
                    else ->
                        Triple("unknown", "Не удалось определить причину автоматически.",
                            "Проверь текст ошибки и состояние файлов; повторяй шаг только после проверки.")
                }
                Finding(record.stepIndex, finding.first, finding.second, finding.third)
            }
    }

    fun report(taskId: String): String {
        val findings = analyze(taskId)
        if (findings.isEmpty()) return "В журнале этой задачи нет зарегистрированных ошибок."
        return findings.mapIndexed { index, finding ->
            "${index + 1}. Шаг ${finding.stepIndex + 1}: ${finding.explanation}\n${finding.suggestion}"
        }.joinToString("\n\n")
    }
}
