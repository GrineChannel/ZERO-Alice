package com.zeroproject.alice.ai

data class AliceTaskStep(
    val index: Int,
    val description: String,
    val requiresApproval: Boolean = false
)

data class AliceTaskPlan(
    val taskId: String,
    val steps: List<AliceTaskStep>
)

/**
 * Deterministic starter planner. A model-backed planner can replace this
 * without changing the task and step contracts.
 */
class AliceTaskPlanner {
    fun plan(task: AliceTask): AliceTaskPlan {
        val descriptions = when (task.capability) {
            AliceCapability.PROGRAMMING -> listOf(
                "Изучить требования и доступные файлы",
                "Подготовить безопасный план изменений",
                "Создать или изменить код",
                "Запустить доступные проверки",
                "Проверить результат и подготовить отчёт"
            )
            AliceCapability.MUSIC -> listOf(
                "Уточнить музыкальную задачу и ограничения",
                "Подготовить текстовую или музыкальную концепцию",
                "Выбрать доступный инструмент для создания",
                "Проверить результат и предложить улучшения"
            )
            AliceCapability.RESEARCH -> listOf(
                "Определить вопросы исследования",
                "Собрать информацию из разрешённых источников",
                "Проверить факты и противоречия",
                "Подготовить выводы с указанием неопределённости"
            )
            AliceCapability.ASSISTANT -> listOf(
                "Определить нужное действие",
                "Проверить доступность инструмента и разрешения",
                "Выполнить разрешённое действие",
                "Подтвердить результат"
            )
            else -> listOf(
                "Определить цель запроса",
                "Подготовить ответ или предложение",
                "Проверить полноту результата"
            )
        }
        return AliceTaskPlan(
            taskId = task.id,
            steps = descriptions.mapIndexed { index, description ->
                AliceTaskStep(
                    index = index + 1,
                    description = description,
                    requiresApproval = task.capability == AliceCapability.PROGRAMMING &&
                        index == 2
                )
            }
        )
    }
}
