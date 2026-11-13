package com.zeroproject.alice.ai

/**
 * Tracks agent progress without invoking any external side effects.
 * Each step must be explicitly recorded after its real-world result is checked.
 */
class AliceAgentSession(
    val prepared: AliceAgentManager.PreparedTask,
    private val store: AliceTaskStore,
    private val evaluator: AliceTaskEvaluator = AliceTaskEvaluator()
) {
    private val completed = mutableSetOf<Int>()

    init { store.save(prepared.task.copy(status = AliceTaskStatus.PLANNED)) }

    @Synchronized
    fun start() {
        val task = store.find(prepared.task.id) ?: return
        if (task.status == AliceTaskStatus.PLANNED) {
            store.changeStatus(task.id, AliceTaskStatus.RUNNING)
        }
    }

    @Synchronized
    fun markStepCompleted(index: Int): Boolean {
        val task = store.find(prepared.task.id) ?: return false
        if (task.status != AliceTaskStatus.RUNNING) return false
        if (prepared.plan.steps.none { it.index == index }) return false
        completed.add(index)
        return true
    }

    @Synchronized
    fun evaluate(): AliceEvaluation = evaluator.evaluate(prepared.plan, completed.toSet())

    @Synchronized
    fun finish(verified: Boolean): Boolean {
        val task = store.find(prepared.task.id) ?: return false
        if (task.status != AliceTaskStatus.RUNNING) return false
        if (!verified || !evaluate().passed) return false
        store.changeStatus(task.id, AliceTaskStatus.COMPLETED)
        return true
    }

    @Synchronized
    fun cancel() {
        val task = store.find(prepared.task.id) ?: return
        if (task.status != AliceTaskStatus.COMPLETED) {
            store.changeStatus(task.id, AliceTaskStatus.CANCELLED)
        }
    }
}
