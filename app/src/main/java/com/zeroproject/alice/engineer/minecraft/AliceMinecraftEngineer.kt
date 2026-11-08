
package com.zeroproject.alice.engineer.minecraft

import android.content.Context
import com.zeroproject.alice.AliceLocalEngine
import com.zeroproject.alice.engineer.AliceEngineerCore
import com.zeroproject.alice.engineer.EngineerTask
import com.zeroproject.alice.engineer.EngineerTaskStatus

data class MinecraftEngineerResult(
    val task: EngineerTask,
    val project: MinecraftProject,
    val plan: EngineerPlan,
    val testReport: MinecraftTestReport
)

class AliceMinecraftEngineer(context: Context) {

    private val appContext = context.applicationContext

    val projectManager = MinecraftProjectManager(appContext)

    val taskCore = AliceEngineerCore(
        appContext,
        projectManager
    )

    val localEngine = AliceLocalEngine(appContext)

    private val planner = AliceEngineerPlanner(localEngine)

    private val codeGenerator =
        AliceEngineerCodeGenerator(localEngine)

    private val fileWriter =
        AliceEngineerFileWriter(projectManager)

    private val testLab = AliceMinecraftTestLab()

    fun initialize(
        onReady: () -> Unit,
        onError: (String) -> Unit
    ) {
        localEngine.initialize(
            onReady = onReady,
            onError = onError
        )
    }

    fun createProject(
        name: String,
        description: String = ""
    ): MinecraftProject {
        return projectManager.createProject(
            name,
            description
        )
    }

    fun develop(
        projectId: String,
        goal: String,
        minecraftVersion: String = "1.21.43",
        scriptApiVersion: String = "1.16.0-beta",
        onProgress: (String) -> Unit,
        onResult: (MinecraftEngineerResult) -> Unit,
        onError: (String) -> Unit
    ) {

        if (!localEngine.isReady()) {
            onError("Локальная модель Элис ещё не готова")
            return
        }

        val project = projectManager.getProject(projectId)

        if (project == null) {
            onError("Проект не найден")
            return
        }

        if (goal.isBlank()) {
            onError("Задача разработки не указана")
            return
        }

        val task = try {
            taskCore.submitTask(projectId, goal)
        } catch (error: Exception) {
            onError(error.message ?: "Не удалось создать задачу")
            return
        }

        try {
            taskCore.updateTaskStatus(
                task.id,
                EngineerTaskStatus.PLANNING,
                "Создание плана разработки"
            )

            onProgress("Элис анализирует задачу и составляет план")

            planner.createPlan(
                goal = goal,
                minecraftVersion = minecraftVersion,
                scriptApiVersion = scriptApiVersion,
                onResult = { plan ->

                    if (plan.steps.isEmpty()) {
                        failTask(
                            task.id,
                            "План разработки не содержит шагов",
                            onError
                        )
                        return@createPlan
                    }

                    onProgress(
                        "План готов. Этапов: ${plan.steps.size}"
                    )

                    processStep(
                        taskId = task.id,
                        project = project,
                        goal = goal,
                        plan = plan,
                        stepIndex = 0,
                        minecraftVersion = minecraftVersion,
                        scriptApiVersion = scriptApiVersion,
                        onProgress = onProgress,
                        onResult = onResult,
                        onError = onError
                    )
                },
                onError = { error ->
                    failTask(task.id, error, onError)
                }
            )

        } catch (error: Exception) {
            failTask(
                task.id,
                error.message ?: "Ошибка запуска разработки",
                onError
            )
        }
    }

    private fun processStep(
        taskId: String,
        project: MinecraftProject,
        goal: String,
        plan: EngineerPlan,
        stepIndex: Int,
        minecraftVersion: String,
        scriptApiVersion: String,
        onProgress: (String) -> Unit,
        onResult: (MinecraftEngineerResult) -> Unit,
        onError: (String) -> Unit
    ) {

        if (stepIndex >= plan.steps.size) {
            finishDevelopment(
                taskId,
                project,
                plan,
                onProgress,
                onResult,
                onError
            )
            return
        }

        val step = plan.steps[stepIndex]

        try {
            taskCore.updateTaskStatus(
                taskId,
                EngineerTaskStatus.GENERATING,
                "Генерация этапа ${stepIndex + 1}: ${step.title}"
            )

            val progress = (
                10 + (stepIndex * 70 / plan.steps.size)
            ).coerceIn(10, 80)

            taskCore.updateProgress(
                taskId,
                progress,
                step.title
            )

            onProgress(
                "Этап ${stepIndex + 1}/${plan.steps.size}: ${step.title}"
            )

            codeGenerator.generateFiles(
                goal = goal,
                minecraftVersion = minecraftVersion,
                scriptApiVersion = scriptApiVersion,
                step = step,
                onResult = { files ->

                    try {
                        if (files.isEmpty()) {
                            failTask(
                                taskId,
                                "Генератор не создал файлы для этапа ${step.title}",
                                onError
                            )
                            return@generateFiles
                        }

                        fileWriter.writeFiles(
                            projectId = project.id,
                            files = files
                        )

                        onProgress(
                            "Сохранено файлов: ${files.size}"
                        )

                        processStep(
                            taskId = taskId,
                            project = project,
                            goal = goal,
                            plan = plan,
                            stepIndex = stepIndex + 1,
                            minecraftVersion = minecraftVersion,
                            scriptApiVersion = scriptApiVersion,
                            onProgress = onProgress,
                            onResult = onResult,
                            onError = onError
                        )

                    } catch (error: Exception) {
                        failTask(
                            taskId,
                            error.message ?: "Ошибка сохранения файлов",
                            onError
                        )
                    }
                },
                onError = { error ->
                    failTask(taskId, error, onError)
                }
            )

        } catch (error: Exception) {
            failTask(
                taskId,
                error.message ?: "Ошибка этапа разработки",
                onError
            )
        }
    }

    private fun finishDevelopment(
        taskId: String,
        project: MinecraftProject,
        plan: EngineerPlan,
        onProgress: (String) -> Unit,
        onResult: (MinecraftEngineerResult) -> Unit,
        onError: (String) -> Unit
    ) {

        try {
            taskCore.updateTaskStatus(
                taskId,
                EngineerTaskStatus.VALIDATING,
                "Проверка проекта"
            )

            taskCore.updateProgress(
                taskId,
                85,
                "Проверка файлов"
            )

            onProgress("Проверка Minecraft Add-on")

            taskCore.updateTaskStatus(
                taskId,
                EngineerTaskStatus.BUILDING,
                "Сборка Add-on"
            )

            taskCore.updateTaskStatus(
                taskId,
                EngineerTaskStatus.TESTING,
                "Тестирование результата"
            )

            val report = testLab.runTests(
                project = project,
                buildAddon = true
            )

            if (report.status == MinecraftTestStatus.FAILED) {
                val details = report.steps
                    .filter {
                        it.status == MinecraftTestStatus.FAILED
                    }
                    .joinToString("\n") {
                        "${it.name}: ${it.details}"
                    }

                failTask(
                    taskId,
                    details.ifBlank { "Тестирование не пройдено" },
                    onError
                )
                return
            }

            taskCore.updateProgress(
                taskId,
                100,
                "Разработка завершена"
            )

            val completedTask = taskCore.updateTaskStatus(
                taskId,
                EngineerTaskStatus.COMPLETED,
                "Add-on успешно собран и проверен"
            )

            onProgress("Разработка завершена")

            onResult(
                MinecraftEngineerResult(
                    task = completedTask,
                    project = project,
                    plan = plan,
                    testReport = report
                )
            )

        } catch (error: Exception) {
            failTask(
                taskId,
                error.message ?: "Ошибка завершения разработки",
                onError
            )
        }
    }

    private fun failTask(
        taskId: String,
        message: String,
        onError: (String) -> Unit
    ) {
        try {
            taskCore.updateTaskStatus(
                taskId,
                EngineerTaskStatus.FAILED,
                message
            )
        } catch (_: Exception) {
            // Сохраняем исходную ошибку
        }

        onError(message)
    }

    fun close() {
        localEngine.close()
    }
}
