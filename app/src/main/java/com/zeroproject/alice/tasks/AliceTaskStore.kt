package com.zeroproject.alice.tasks

import android.content.Context
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
    private val lock = Any()

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

    fun advance(id: String): Task? = synchronized(lock) {
        val tasks = readAll().toMutableList()
        val index = tasks.indexOfFirst { it.id == id }
        if (index < 0) return@synchronized null
        val current = tasks[index]
        if (current.status == "cancelled") return@synchronized current
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
        if (!file.exists()) return emptyList()
        val array = JSONArray(file.readText(Charsets.UTF_8))
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
        val temp = File(file.parentFile, "${file.name}.tmp")
        temp.writeText(array.toString(), Charsets.UTF_8)
        if (!temp.renameTo(file)) {
            temp.delete()
            throw IllegalStateException("Не удалось сохранить задачи")
        }
    }
}
