
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
        AliceMinecraftAddonBuilder(validator),

    private val archiveVerifier: AliceMinecraftArchiveVerifier =
        AliceMinecraftArchiveVerifier()
) {

    fun runTests(
        project: MinecraftProject,
        buildAddon: Boolean = true
    ): MinecraftTestReport {

        val steps = mutableListOf<MinecraftTestStep>()
        var addonFile: File? = null

        if (!project.directory.exists() ||
            !project.directory.isDirectory
        ) {
            steps.add(
                MinecraftTestStep(
                    "Структура проекта",
                    MinecraftTestStatus.FAILED,
                    "Папка проекта не найдена"
                )
            )

            return createReport(project, steps, null)
        }

        steps.add(
            MinecraftTestStep(
                "Структура проекта",
                MinecraftTestStatus.PASSED,
                "Папка проекта доступна"
            )
        )

        val validation = try {
            validator.validateProject(project)
        } catch (error: Exception) {
            steps.add(
                MinecraftTestStep(
                    "Проверка Add-on",
                    MinecraftTestStatus.FAILED,
                    error.message ?: "Ошибка проверки проекта"
                )
            )

            return createReport(project, steps, null)
        }

        val errors = validation.issues.filter {
            it.severity == ValidationSeverity.ERROR
        }

        val warnings = validation.issues.filter {
            it.severity == ValidationSeverity.WARNING
        }

        if (errors.isNotEmpty()) {
            steps.add(
                MinecraftTestStep(
                    "Проверка Add-on",
                    MinecraftTestStatus.FAILED,
                    errors.joinToString("\n") {
                        "${it.file}: ${it.message}"
                    }
                )
            )

            return createReport(project, steps, null)
        }

        steps.add(
            MinecraftTestStep(
                "Проверка Add-on",
                if (warnings.isEmpty()) {
                    MinecraftTestStatus.PASSED
                } else {
                    MinecraftTestStatus.WARNING
                },
                if (warnings.isEmpty()) {
                    "Ошибок и предупреждений не обнаружено"
                } else {
                    warnings.joinToString("\n") {
                        "${it.file}: ${it.message}"
                    }
                }
            )
        )

        if (!buildAddon) {
            return createReport(project, steps, null)
        }

        try {
            val result = builder.build(project)
            addonFile = result.addonFile

            steps.add(
                MinecraftTestStep(
                    "Сборка Add-on",
                    MinecraftTestStatus.PASSED,
                    "Создан архив: ${result.addonFile.name}"
                )
            )

        } catch (error: Exception) {
            steps.add(
                MinecraftTestStep(
                    "Сборка Add-on",
                    MinecraftTestStatus.FAILED,
                    error.message ?: "Ошибка сборки"
                )
            )

            return createReport(project, steps, null)
        }

        // Проверка итогового архива

        val verification = try {
            archiveVerifier.verifyAddon(addonFile!!)
        } catch (error: Exception) {
            steps.add(
                MinecraftTestStep(
                    "Проверка архива",
                    MinecraftTestStatus.FAILED,
                    error.message ?: "Ошибка проверки архива"
                )
            )

            return createReport(project, steps, addonFile)
        }

        if (verification.valid) {
            steps.add(
                MinecraftTestStep(
                    "Проверка архива",
                    MinecraftTestStatus.PASSED,
                    "Проверено пакетов: ${verification.checkedPacks}. " +
                        "Файлов во внешнем архиве: ${verification.checkedFiles}."
                )
            )
        } else {
            steps.add(
                MinecraftTestStep(
                    "Проверка архива",
                    MinecraftTestStatus.FAILED,
                    verification.errors.joinToString("\n")
                )
            )
        }

        return createReport(project, steps, addonFile)
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
