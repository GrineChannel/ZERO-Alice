package com.zeroproject.alice.ai

/**
 * Safe orchestration scaffold. Planning does not execute tools or call models.
 * Tool execution and persistence will be introduced as separate components.
 */
class AliceAgentManager(
    private val intelligence: AliceIntelligenceCore = AliceIntelligenceCore(),
    private val planner: AliceTaskPlanner = AliceTaskPlanner(),
    val tools: AliceToolRegistry = AliceToolRegistry()
) {
    data class PreparedTask(
        val task: AliceTask,
        val plan: AliceTaskPlan
    )

    fun prepare(
        request: String,
        cloudAvailable: Boolean = false,
        cloudAllowed: Boolean = false
    ): PreparedTask {
        val task = intelligence.createTask(
            request = request,
            cloudAvailable = cloudAvailable,
            cloudAllowed = cloudAllowed
        )
        return PreparedTask(task = task, plan = planner.plan(task))
    }

    fun describe(prepared: PreparedTask): String = buildString {
        appendLine(intelligence.describeTask(prepared.task))
        appendLine("Plan:")
        prepared.plan.steps.forEach { step ->
            append(step.index)
            append(". ")
            append(step.description)
            if (step.requiresApproval) append(" [требуется подтверждение]")
            appendLine()
        }
    }
}
