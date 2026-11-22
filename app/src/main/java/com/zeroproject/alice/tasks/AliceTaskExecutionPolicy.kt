package com.zeroproject.alice.tasks

/**
 * Pure task-execution decisions, shared by the executor and JVM tests.
 * This class does not perform I/O or grant permissions.
 */
object AliceTaskExecutionPolicy {
    fun canAdvance(status: String, completedSteps: Int, expectedStep: Int, totalSteps: Int): Boolean =
        status != "cancelled" && status != "completed" && status != "paused" &&
            expectedStep >= 0 && expectedStep < totalSteps &&
            completedSteps == expectedStep

    fun requiresSingleStep(step: String): Boolean {
        val value = step.trim().lowercase()
        return value.startsWith("предпросмотр замены элис ") ||
            value.startsWith("замени файл элис ") ||
            value.startsWith("откати файл элис ") ||
            value.startsWith("разработай minecraft элис ")
    }
}
