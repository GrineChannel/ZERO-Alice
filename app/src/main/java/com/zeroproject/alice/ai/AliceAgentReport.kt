package com.zeroproject.alice.ai

/** Human-readable reporting based on persisted evidence. */
class AliceAgentReport {
    fun render(task: AliceTask, events: List<AliceAgentEvent>): String = buildString {
        appendLine("Задача: ${task.request}")
        appendLine("Статус: ${task.status}")
        appendLine("Режим: ${task.mode}")
        appendLine("Событий: ${events.size}")
        events.take(20).forEach { event ->
            appendLine("- ${event.kind}: ${event.details.take(200)}")
        }
        if (task.status != AliceTaskStatus.COMPLETED) {
            appendLine("Задача ещё не подтверждена как выполненная.")
        }
    }
}
