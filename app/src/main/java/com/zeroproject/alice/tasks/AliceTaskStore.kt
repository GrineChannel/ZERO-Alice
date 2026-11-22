package com.zeroproject.alice.tasks

import android.content.Context
import android.util.AtomicFile
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/**
 * Persistent task plans. No background execution is implied: this class
 * records goals, steps and progress for later orchestration.
 */
class AliceTaskStore(context: Context) {
    private val file = File(context.filesDir, "alice_tasks.json")
    private val atomicFile = AtomicFile(file)
    companion object {
        // Task stores created by separate chat commands share one file lock.
        private val lock = Any()
    }

    data class Task(
        val id: String,
        val goal: String,
        val steps: List<String>,
        val completedSteps: Int,
        val status: String,
        val updatedAt: Long
    )

    fun list(): List<Task> = synchronized(lock) { readAll() }

    fun create(goal: String, steps: List<String>): Task = synchronized(lock) {
        require(goal.isNotBlank()) { "Укажи цель" }
        require(goal.length <= 2000) { "Слишком длинная цель" }
        require(steps.isNotEmpty() && steps.size <= 50) { "Требуется от 1 до 50 шагов" }
        require(steps.all { it.isNotBlank() && it.length <= 2000 }) { "Некорректный шаг" }
        val task = Task(
            UUID.randomUUID().toString(),
            goal.trim(),
            steps.map { it.trim() },
            0,
            "planned",
            System.currentTimeMillis()
        )
        val tasks = readAll().toMutableList()
        tasks.add(task)
        saveAll(tasks)
        task
    }

    /** Create a fresh plan from an existing task without copying execution state. */
    fun duplicate(id: String): Task? = synchronized(lock) {
        val tasks = readAll().toMutableList()
        val original = tasks.firstOrNull { it.id == id } ?: return@synchronized null
        val copy = Task(
            UUID.randomUUID().toString(),
            original.goal,
            original.steps.toList(),
            0,
            "planned",
            System.currentTimeMillis()
        )
        tasks.add(copy)
        saveAll(tasks)
        copy
    }

    fun advance(id: String): Task? = synchronized(lock) {
        val tasks = readAll().toMutableList()
        val index = tasks.indexOfFirst { it.id == id }
        if (index < 0) return@synchronized null
        val current = tasks[index]
        if (current.status == "cancelled" || current.status == "completed") return@synchronized current
        val next = (current.completedSteps + 1).coerceAtMost(current.steps.size)
        val updated = current.copy(
            completedSteps = next,
            status = if (next == current.steps.size) "completed" else "in_progress",
            updatedAt = System.currentTimeMillis()
        )
        tasks[index] = updated
        saveAll(tasks)
        updated
    }

    /** Advance only when the caller still owns the expected pending step. */
    fun advanceIfCurrent(id: String, expectedStep: Int): Task? = synchronized(lock) {
        val tasks = readAll().toMutableList()
        val index = tasks.indexOfFirst { it.id == id }
        if (index < 0) return@synchronized null
        val current = tasks[index]
        if (!AliceTaskExecutionPolicy.canAdvance(
                current.status, current.completedSteps, expectedStep, current.steps.size
            )) {
            return@synchronized null
        }
        val next = expectedStep + 1
        val updated = current.copy(
            completedSteps = next,
            status = if (next == current.steps.size) "completed" else "in_progress",
            updatedAt = System.currentTimeMillis()
        )
        tasks[index] = updated
        saveAll(tasks)
        updated
    }

    /** Move a non-terminal plan up or down in the persisted task queue. */
    fun move(id: String, direction: Int): Boolean = synchronized(lock) {
        require(direction == -1 || direction == 1) { "Направление должно быть -1 или 1" }
        val tasks = readAll().toMutableList()
        val index = tasks.indexOfFirst { it.id == id }
        if (index < 0 || tasks[index].status == "completed" ||
            tasks[index].status == "cancelled") return@synchronized false
        val target = index + direction
        if (target !in tasks.indices) return@synchronized false
        val other = tasks[target]
        if (other.status == "completed" || other.status == "cancelled") {
            return@synchronized false
        }
        java.util.Collections.swap(tasks, index, target)
        saveAll(tasks)
        true
    }

    /** Queue order is stable across app restarts; this returns runnable plans only. */
    fun queue(): List<Task> = synchronized(lock) {
        readAll().filter { it.status == "planned" || it.status == "in_progress" }
    }

    /** Pause a plan without losing completed steps. */
    fun pause(id: String): Task? = synchronized(lock) {
        val tasks = readAll().toMutableList()
        val index = tasks.indexOfFirst { it.id == id }
        if (index < 0) return@synchronized null
        val current = tasks[index]
        if (current.status == "completed" || current.status == "cancelled" ||
            current.status == "paused") return@synchronized current
        val updated = current.copy(status = "paused", updatedAt = System.currentTimeMillis())
        tasks[index] = updated
        saveAll(tasks)
        updated
    }

    /** Resume a paused plan; does not automatically execute any steps. */
    fun resume(id: String): Task? = synchronized(lock) {
        val tasks = readAll().toMutableList()
        val index = tasks.indexOfFirst { it.id == id }
        if (index < 0) return@synchronized null
        val current = tasks[index]
        if (current.status != "paused") return@synchronized current
        val updated = current.copy(
            status = if (current.completedSteps == 0) "planned" else "in_progress",
            updatedAt = System.currentTimeMillis()
        )
        tasks[index] = updated
        saveAll(tasks)
        updated
    }

    fun cancel(id: String): Task? = synchronized(lock) {
        val tasks = readAll().toMutableList()
        val index = tasks.indexOfFirst { it.id == id }
        if (index < 0) return@synchronized null
        val current = tasks[index]
        if (current.status == "completed") return@synchronized current
        val updated = current.copy(status = "cancelled", updatedAt = System.currentTimeMillis())
        tasks[index] = updated
        saveAll(tasks)
        updated
    }

    private fun readAll(): List<Task> {
        if (!file.exists() && !File(file.path + ".bak").exists()) return emptyList()
        val array = JSONArray(atomicFile.openRead().bufferedReader(Charsets.UTF_8).use { it.readText() })
        return (0 until array.length()).map { index ->
            val item = array.getJSONObject(index)
            val steps = item.getJSONArray("steps")
            Task(
                item.getString("id"),
                item.getString("goal"),
                (0 until steps.length()).map { steps.getString(it) },
                item.getInt("completedSteps"),
                item.getString("status"),
                item.getLong("updatedAt")
            )
        }
    }

    private fun saveAll(tasks: List<Task>) {
        val array = JSONArray()
        tasks.forEach { task ->
            val item = JSONObject()
            item.put("id", task.id)
            item.put("goal", task.goal)
            item.put("steps", JSONArray(task.steps))
            item.put("completedSteps", task.completedSteps)
            item.put("status", task.status)
            item.put("updatedAt", task.updatedAt)
            array.put(item)
        }
        val stream = atomicFile.startWrite()
        try {
            stream.write(array.toString().toByteArray(Charsets.UTF_8))
            atomicFile.finishWrite(stream)
        } catch (error: Exception) {
            atomicFile.failWrite(stream)
            throw error
        }
    }
}
