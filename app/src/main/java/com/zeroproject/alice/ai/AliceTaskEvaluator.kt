package com.zeroproject.alice.ai

data class AliceEvaluation(
    val passed: Boolean,
    val notes: List<String>
)

/** Checks execution evidence; never treats a generated plan as completed work. */
class AliceTaskEvaluator {
    fun evaluate(plan: AliceTaskPlan, completedStepIndexes: Set<Int>): AliceEvaluation {
        val missing = plan.steps.filterNot { it.index in completedStepIndexes }
        return AliceEvaluation(
            passed = plan.steps.isNotEmpty() && missing.isEmpty(),
            notes = if (missing.isEmpty() && plan.steps.isNotEmpty()) {
                listOf("All steps marked complete; external result verification still required")
            } else {
                missing.map { "Step ${it.index} not completed: ${it.description}" }
            }
        )
    }
}
