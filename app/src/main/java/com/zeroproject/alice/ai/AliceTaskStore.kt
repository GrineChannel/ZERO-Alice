package com.zeroproject.alice.ai

import java.util.concurrent.ConcurrentHashMap

/** In-memory task state; durable persistence will be added separately. */
class AliceTaskStore {
    private val tasks = ConcurrentHashMap<String, AliceTask>()
    fun save(task: AliceTask) { tasks[task.id] = task }
    fun find(id: String): AliceTask? = tasks[id]
    fun all(): List<AliceTask> = tasks.values.sortedByDescending { it.createdAt }
    fun changeStatus(id: String, status: AliceTaskStatus): AliceTask? =
        tasks.computeIfPresent(id) { _, old -> old.copy(status = status) }
    fun remove(id: String): Boolean = tasks.remove(id) != null
}
