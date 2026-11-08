
package com.zeroproject.alice.engineer

import android.content.Context
import com.zeroproject.alice.engineer.minecraft.MinecraftProjectManager
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

enum class EngineerTaskStatus {
    QUEUED,
    PLANNING,
    GENERATING,
    VALIDATING,
    BUILDING,
    TESTING,
    COMPLETED,
    FAILED,
    CANCELLED
}

data class EngineerTask(
    val id: String,
    val projectId: String,
    val goal: String,
    val status: EngineerTaskStatus,
    val progress: Int,
    val message: String,
    val createdAt: Long,
    val updatedAt: Long
)

class AliceEngineerCore(
    context: Context,
    private val projectManager: MinecraftProjectManager
) {
    private val appContext = context.applicationContext

    private val engineerDirectory = File(
        appContext.filesDir,
        "engineer"
    )

    private val tasksFile = File(
        engineerDirectory,
        "tasks.json"
    )

    private val tasks = mutableListOf<EngineerTask>()

    init {
        engineerDirectory.mkdirs()
        tasks.addAll(loadTasks())
    }

    @Synchronized
    fun submitTask(
        projectId: String,
        goal: String
    ): EngineerTask {
        require(projectManager.getProject(projectId) != null) {
            "Проект Minecraft не найден"
        }

        val cleanGoal = goal.trim()

        require(cleanGoal.isNotBlank()) {
            "Описание задачи не может быть пустым"
        }

        require(cleanGoal.length <= 2000) {
            "Описание задачи слишком длинное"
        }

        val now = System.currentTimeMillis()

        val task = EngineerTask(
            id = UUID.randomUUID().toString(),
            projectId = projectId,
            goal = cleanGoal,
            status = EngineerTaskStatus.QUEUED,
            progress = 0,
            message = "Задача добавлена в очередь",
            createdAt = now,
            updatedAt = now
        )

        tasks.add(task)
        saveTasks()

        return task
    }

    @Synchronized
    fun getTasks(): List<EngineerTask> {
        return tasks.sortedByDescending { it.createdAt }
    }

    @Synchronized
    fun getTask(taskId: String): EngineerTask? {
        return tasks.firstOrNull { it.id == taskId }
    }

    @Synchronized
    fun updateTaskStatus(
        taskId: String,
        newStatus: EngineerTaskStatus,
        message: String = ""
    ): EngineerTask {
        val index = tasks.indexOfFirst { it.id == taskId }

        require(index >= 0) {
            "Инженерная задача не найдена"
        }

        val current = tasks[index]

        check(!isTerminal(current.status)) {
            "Задача уже завершена"
        }

        check(isAllowedTransition(current.status, newStatus)) {
            "Недопустимый переход: ${current.status} -> $newStatus"
        }

        val updated = current.copy(
            status = newStatus,
            progress = if (newStatus == EngineerTaskStatus.COMPLETED) {
                100
            } else {
                current.progress
            },
            message = message.ifBlank { defaultMessage(newStatus) },
            updatedAt = System.currentTimeMillis()
        )

        tasks[index] = updated
        saveTasks()

        return updated
    }

    @Synchronized
    fun updateProgress(
        taskId: String,
        progress: Int,
        message: String = ""
    ): EngineerTask {
        val index = tasks.indexOfFirst { it.id == taskId }

        require(index >= 0) {
            "Инженерная задача не найдена"
        }

        val current = tasks[index]

        check(!isTerminal(current.status)) {
            "Нельзя изменить завершённую задачу"
        }

        val updated = current.copy(
            progress = progress.coerceIn(0, 100),
            message = message.ifBlank { current.message },
            updatedAt = System.currentTimeMillis()
        )

        tasks[index] = updated
        saveTasks()

        return updated
    }

    @Synchronized
    fun cancelTask(taskId: String): EngineerTask {
        return updateTaskStatus(
            taskId,
            EngineerTaskStatus.CANCELLED,
            "Задача отменена пользователем"
        )
    }

    @Synchronized
    fun getActiveTasks(): List<EngineerTask> {
        return tasks.filter { !isTerminal(it.status) }
    }

    private fun isTerminal(status: EngineerTaskStatus): Boolean {
        return status == EngineerTaskStatus.COMPLETED ||
            status == EngineerTaskStatus.FAILED ||
            status == EngineerTaskStatus.CANCELLED
    }

    private fun isAllowedTransition(
        current: EngineerTaskStatus,
        next: EngineerTaskStatus
    ): Boolean {
        if (next == EngineerTaskStatus.FAILED ||
            next == EngineerTaskStatus.CANCELLED
        ) {
            return true
        }

        return when (current) {
            EngineerTaskStatus.QUEUED ->
                next == EngineerTaskStatus.PLANNING

            EngineerTaskStatus.PLANNING ->
                next == EngineerTaskStatus.GENERATING

            EngineerTaskStatus.GENERATING ->
                next == EngineerTaskStatus.VALIDATING

            EngineerTaskStatus.VALIDATING ->
                next == EngineerTaskStatus.GENERATING ||
                    next == EngineerTaskStatus.BUILDING

            EngineerTaskStatus.BUILDING ->
                next == EngineerTaskStatus.TESTING

            EngineerTaskStatus.TESTING ->
                next == EngineerTaskStatus.GENERATING ||
                    next == EngineerTaskStatus.COMPLETED

            else -> false
        }
    }

    private fun defaultMessage(
        status: EngineerTaskStatus
    ): String {
        return when (status) {
            EngineerTaskStatus.QUEUED -> "Ожидание запуска"
            EngineerTaskStatus.PLANNING -> "Составление плана"
            EngineerTaskStatus.GENERATING -> "Создание файлов проекта"
            EngineerTaskStatus.VALIDATING -> "Проверка файлов и кода"
            EngineerTaskStatus.BUILDING -> "Сборка дополнения"
            EngineerTaskStatus.TESTING -> "Тестирование результата"
            EngineerTaskStatus.COMPLETED -> "Задача успешно завершена"
            EngineerTaskStatus.FAILED -> "При выполнении возникла ошибка"
            EngineerTaskStatus.CANCELLED -> "Задача отменена"
        }
    }

    private fun saveTasks() {
        val array = JSONArray()

        tasks.forEach { task ->
            array.put(JSONObject().apply {
                put("id", task.id)
                put("projectId", task.projectId)
                put("goal", task.goal)
                put("status", task.status.name)
                put("progress", task.progress)
                put("message", task.message)
                put("createdAt", task.createdAt)
                put("updatedAt", task.updatedAt)
            })
        }

        val temporaryFile = File(
            engineerDirectory,
            "tasks.tmp"
        )

        temporaryFile.writeText(
            array.toString(2),
            Charsets.UTF_8
        )

        if (tasksFile.exists() && !tasksFile.delete()) {
            throw IllegalStateException(
                "Не удалось обновить хранилище задач"
            )
        }

        check(temporaryFile.renameTo(tasksFile)) {
            "Не удалось сохранить инженерные задачи"
        }
    }

    private fun loadTasks(): List<EngineerTask> {
        if (!tasksFile.isFile) return emptyList()

        return try {
            val array = JSONArray(
                tasksFile.readText(Charsets.UTF_8)
            )

            val result = mutableListOf<EngineerTask>()

            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)

                result.add(
                    EngineerTask(
                        id = item.getString("id"),
                        projectId = item.getString("projectId"),
                        goal = item.getString("goal"),
                        status = EngineerTaskStatus.valueOf(
                            item.getString("status")
                        ),
                        progress = item.optInt("progress", 0)
                            .coerceIn(0, 100),
                        message = item.optString("message"),
                        createdAt = item.optLong("createdAt"),
                        updatedAt = item.optLong("updatedAt")
                    )
                )
            }

            result
        } catch (_: Exception) {
            emptyList()
        }
    }
}
