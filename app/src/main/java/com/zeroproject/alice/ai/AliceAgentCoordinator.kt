package com.zeroproject.alice.ai

/**
 * Persistence-backed preparation. No background execution is implied.
 * Call from a worker thread when using SQLite.
 */
class AliceAgentCoordinator(
    private val agent: AliceAgentManager,
    private val store: AlicePersistentTaskStore,
    private val journal: AliceAgentJournal
) {
    fun submit(
        request: String,
        cloudAvailable: Boolean = false,
        cloudAllowed: Boolean = false
    ): AliceAgentManager.PreparedTask {
        val prepared = agent.prepare(request, cloudAvailable, cloudAllowed)
        store.save(prepared.task.copy(status = AliceTaskStatus.PLANNED))
        journal.record(prepared.task.id, "planned", agent.describe(prepared))
        return prepared
    }

    fun cancel(taskId: String): Boolean {
        val current = store.find(taskId) ?: return false
        if (current.status == AliceTaskStatus.COMPLETED ||
            current.status == AliceTaskStatus.CANCELLED) return false
        val changed = store.updateStatus(taskId, AliceTaskStatus.CANCELLED)
        if (changed) journal.record(taskId, "cancelled", "Cancelled by request")
        return changed
    }
}
