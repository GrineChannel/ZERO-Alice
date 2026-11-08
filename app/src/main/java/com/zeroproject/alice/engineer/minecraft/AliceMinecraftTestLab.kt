
package com.zeroproject.alice.engineer.minecraft

import java.io.File

enum class MinecraftTestStatus {
    PASSED,
    WARNING,
    FAILED
}

data class MinecraftTestStep(
    val name: String,
    val status: MinecraftTestStatus,
    val details: String
)

data class MinecraftTestReport(
    val projectName: String,
    val status: MinecraftTestStatus,
    val steps: List<MinecraftTestStep>,
    val addonFile: File?,
    val completedAt: Long
)

class AliceMinecraftTestLab(
    private val validator: AliceMinecraftValidator =
        AliceMinecraftValidator(),

    private val builder: AliceMinecraftAddonBuilder =
        AliceMinecraftAddonBuilder(validator)
) {

    fun runTests(
        project: MinecraftProject,
        buildAddon: Boolean = true
    ): MinecraftTestReport {

        val steps = mutableListOf<MinecraftTestStep>()
        var addonFile: File? = null

        // 1. Проверка существования проекта

        if (!project.directory.exists() ||
            !project.directory.isDirectory
        ) {

            steps.add(
                MinecraftTestStep(
                    name = "Структура проекта",
                    status = MinecraftTestStatus.FAILED,
                    details = "Папка проекта не найдена"
                )
            )

            return createReport(
                project,
                steps,
                null
            )
        }

        steps.add(
            MinecraftTestStep(
                name = "Структура проекта",
                status = MinecraftTestStatus.PASSED,
                details = "Папка проекта доступна"
            )
        )

        // 2. Проверка Minecraft Add-on

        val validation = try {
            validator.validateProject(project)
        } catch (error: Exception) {

            steps.add(
                MinecraftTestStep(
                    name = "Проверка Add-on",
                    status = MinecraftTestStatus.FAILED,
                    details = error.message
                        ?: "Неизвестная ошибка проверки"
                )
            )

            return createReport(
                project,
                steps,
                null
            )
        }

        val errors = validation.issues.filter {
            it.severity == ValidationSeverity.ERROR
        }

        val warnings = validation.issues.filter {
            it.severity == ValidationSeverity.WARNING
        }

        if (errors.isNotEmpty()) {

            val details = errors.joinToString("\n") {
                "${it.file}: ${it.message}"
            }

            steps.add(
                MinecraftTestStep(
                    name = "Проверка Add-on",
                    status = MinecraftTestStatus.FAILED,
                    details = details
                )
            )

            return createReport(
                project,
                steps,
                null
            )
        }

        steps.add(
            MinecraftTestStep(
                name = "Проверка Add-on",
                status = if (warnings.isEmpty()) {
                    MinecraftTestStatus.PASSED
                } else {
                    MinecraftTestStatus.WARNING
                },
                details = if (warnings.isEmpty()) {
                    "Ошибок и предупреждений не обнаружено"
                } else {
                    warnings.joinToString("\n") {
                        "${it.file}: ${it.message}"
                    }
                }
            )
        )

        // 3. Сборка Add-on

        if (buildAddon) {

            try {

                val result = builder.build(project)

                addonFile = result.addonFile

                steps.add(
                    MinecraftTestStep(
                        name = "Сборка Add-on",
                        status = MinecraftTestStatus.PASSED,
                        details = "Архив создан: ${result.addonFile.name}"
                    )
                )

                // 4. Проверка результата сборки

                val fileExists = result.addonFile.isFile
                val fileNotEmpty = result.addonFile.length() > 0L

                if (fileExists && fileNotEmpty) {

                    steps.add(
                        MinecraftTestStep(
                            name = "Проверка архива",
                            status = MinecraftTestStatus.PASSED,
                            details = "Файл существует и не пуст"
                        )
                    )

                } else {

                    steps.add(
                        MinecraftTestStep(
                            name = "Проверка архива",
                            status = MinecraftTestStatus.FAILED,
                            details = "Созданный архив отсутствует или пуст"
                        )
                    )
                }

            } catch (error: Exception) {

                steps.add(
                    MinecraftTestStep(
                        name = "Сборка Add-on",
                        status = MinecraftTestStatus.FAILED,
                        details = error.message
                            ?: "Ошибка сборки"
                    )
                )
            }
        }

        return createReport(
            project,
            steps,
            addonFile
        )
    }

    private fun createReport(
        project: MinecraftProject,
        steps: List<MinecraftTestStep>,
        addonFile: File?
    ): MinecraftTestReport {

        val status = when {
            steps.any {
                it.status == MinecraftTestStatus.FAILED
            } -> MinecraftTestStatus.FAILED

            steps.any {
                it.status == MinecraftTestStatus.WARNING
            } -> MinecraftTestStatus.WARNING

            else -> MinecraftTestStatus.PASSED
        }

        return MinecraftTestReport(
            projectName = project.name,
            status = status,
            steps = steps,
            addonFile = addonFile,
            completedAt = System.currentTimeMillis()
        )
    }
}
