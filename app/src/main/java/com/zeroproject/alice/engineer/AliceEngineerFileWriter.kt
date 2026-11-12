
package com.zeroproject.alice.engineer

import com.zeroproject.alice.engineer.minecraft.MinecraftProjectManager
import java.io.File
import java.util.UUID

data class EngineerFileWriteResult(
    val writtenFiles: List<String>,
    val backupFiles: List<String>,
    val createdFiles: List<String>
)

class AliceEngineerFileWriter(
    private val projectManager: MinecraftProjectManager
) {

    companion object {
        private const val MAX_FILES = 30
        private const val MAX_FILE_SIZE = 100_000
        private const val MAX_TOTAL_SIZE = 500_000
    }

    private data class PendingFile(
        val path: String,
        val content: String,
        val existedBefore: Boolean,
        val previousContent: String?
    )

    /**
     * Read-only comparison of generated files with existing project files.
     * Does not modify or create any file.
     */
    @Synchronized
    fun previewChanges(
        projectId: String,
        files: List<EngineerGeneratedFile>
    ): String {
        require(files.isNotEmpty() && files.size <= MAX_FILES) {
            "Некорректное количество файлов"
        }
        val project = projectManager.getProject(projectId)
            ?: throw IllegalArgumentException("Minecraft-проект не найден")
        val root = project.directory.canonicalFile
        val seen = mutableSetOf<String>()
        var added = 0
        var modified = 0
        var unchanged = 0
        val lines = mutableListOf<String>()
        for (generated in files) {
            val path = generated.path.trim()
            require(isSafeAddonPath(path) && seen.add(path)) {
                "Недопустимый или повторяющийся путь: $path"
            }
            require(generated.content.length <= MAX_FILE_SIZE) {
                "Слишком большой файл: $path"
            }
            val target = File(root, path).canonicalFile
            require(target.path.startsWith(root.path + File.separator)) {
                "Путь за пределами проекта: $path"
            }
            require(!target.isDirectory) { "Путь является папкой: $path" }
            val state = when {
                !target.exists() -> { added++; "НОВЫЙ" }
                !target.isFile -> throw IllegalArgumentException("Недопустимый файл: $path")
                target.readText(Charsets.UTF_8) == generated.content -> {
                    unchanged++; "БЕЗ ИЗМЕНЕНИЙ"
                }
                else -> { modified++; "ИЗМЕНЁН" }
            }
            lines.add("$state | $path")
        }
        return buildString {
            append("Предпросмотр Minecraft-файлов: новых $added, изменённых $modified, без изменений $unchanged.\\n")
            append(lines.joinToString("\\n"))
            append("\\nФайлы не изменены.")
        }
    }

    /**
     * Save candidate output separately, without touching installed add-on files.
     * Candidates are stored under backups/previews and can be inspected later.
     */
    @Synchronized
    fun stageFiles(
        projectId: String,
        files: List<EngineerGeneratedFile>
    ): String {
        require(files.isNotEmpty() && files.size <= MAX_FILES) {
            "Некорректное количество файлов"
        }
        val project = projectManager.getProject(projectId)
            ?: throw IllegalArgumentException("Minecraft-проект не найден")
        val root = project.directory.canonicalFile
        val batchId = UUID.randomUUID().toString()
        val staged = mutableListOf<String>()
        val seen = mutableSetOf<String>()
        var totalSize = 0
        try {
            for (generated in files) {
                val path = generated.path.trim()
                require(isSafeAddonPath(path) && seen.add(path)) {
                    "Недопустимый или повторяющийся путь: $path"
                }
                require(generated.content.isNotBlank() && generated.content.length <= MAX_FILE_SIZE) {
                    "Некорректный размер файла: $path"
                }
                totalSize += generated.content.length
                require(totalSize <= MAX_TOTAL_SIZE) { "Превышен общий размер файлов" }
                val destination = "backups/previews/$batchId/$path"
                val target = File(root, destination).canonicalFile
                require(target.path.startsWith(root.path + File.separator)) {
                    "Недопустимый путь предварительной версии"
                }
                projectManager.writeFile(projectId, destination, generated.content)
                staged.add(destination)
            }
            return "Предварительная версия $batchId: сохранено ${staged.size} файлов отдельно от дополнения."
        } catch (error: Exception) {
            for (path in staged.asReversed()) {
                try { projectManager.deleteFile(projectId, path) } catch (_: Exception) { }
            }
            throw error
        }
    }

    @Synchronized
    fun writeFiles(
        projectId: String,
        files: List<EngineerGeneratedFile>
    ): EngineerFileWriteResult {

        require(files.isNotEmpty()) {
            "Нет файлов для записи"
        }

        require(files.size <= MAX_FILES) {
            "Слишком много файлов"
        }

        val project = projectManager.getProject(projectId)
            ?: throw IllegalArgumentException(
                "Проект Minecraft не найден"
            )

        val uniquePaths = mutableSetOf<String>()
        var totalSize = 0

        // Сначала проверяем все файлы.
        val pending = files.map { generated ->

            val path = generated.path.trim()

            require(isSafeAddonPath(path)) {
                "Недопустимый путь: $path"
            }

            require(uniquePaths.add(path)) {
                "Повторяющийся путь: $path"
            }

            require(generated.content.isNotBlank()) {
                "Пустой файл: $path"
            }

            require(generated.content.length <= MAX_FILE_SIZE) {
                "Файл слишком большой: $path"
            }

            totalSize += generated.content.length

            require(totalSize <= MAX_TOTAL_SIZE) {
                "Превышен общий размер файлов"
            }

            val target = File(
                project.directory,
                path
            ).canonicalFile

            val root = project.directory.canonicalFile

            require(
                target.path.startsWith(root.path + File.separator)
            ) {
                "Путь выходит за пределы проекта"
            }

            require(!target.isDirectory) {
                "Нельзя перезаписать папку: $path"
            }

            val existed = target.isFile

            val previousContent = if (existed) {
                projectManager.readFile(projectId, path)
            } else {
                null
            }

            PendingFile(
                path = path,
                content = generated.content,
                existedBefore = existed,
                previousContent = previousContent
            )
        }

        // Создаём отдельную папку резервной копии.
        val backupId = UUID.randomUUID().toString()

        val backupPaths = mutableMapOf<String, String>()

        val writtenPaths = mutableListOf<String>()
        val createdPaths = mutableListOf<String>()

        try {

            // Сначала сохраняем предыдущие версии.
            for (file in pending) {

                if (!file.existedBefore) continue

                val backupPath =
                    "backups/auto/$backupId/${file.path}"

                projectManager.writeFile(
                    projectId,
                    backupPath,
                    file.previousContent!!
                )

                backupPaths[file.path] = backupPath
            }

            // Только после создания резервных копий
            // начинаем обновлять файлы проекта.
            for (file in pending) {

                projectManager.writeFile(
                    projectId,
                    file.path,
                    file.content
                )

                writtenPaths.add(file.path)

                if (!file.existedBefore) {
                    createdPaths.add(file.path)
                }
            }

            return EngineerFileWriteResult(
                writtenFiles = writtenPaths.toList(),
                backupFiles = backupPaths.values.toList(),
                createdFiles = createdPaths.toList()
            )

        } catch (error: Throwable) {

            // При ошибке пытаемся вернуть исходное состояние.
            val rollbackErrors = mutableListOf<String>()

            for (path in writtenPaths.asReversed()) {

                try {

                    val original = pending.first {
                        it.path == path
                    }

                    if (original.existedBefore) {

                        projectManager.writeFile(
                            projectId,
                            path,
                            original.previousContent!!
                        )

                    } else {

                        projectManager.deleteFile(
                            projectId,
                            path
                        )
                    }

                } catch (rollbackError: Throwable) {

                    rollbackErrors.add(
                        "$path: ${rollbackError.message}"
                    )
                }
            }

            val rollbackMessage = if (rollbackErrors.isEmpty()) {
                "Исходное состояние восстановлено."
            } else {
                "Ошибки восстановления: ${rollbackErrors.joinToString()}"
            }

            throw IllegalStateException(
                "Ошибка записи файлов: ${error.message}. $rollbackMessage",
                error
            )
        }
    }

    private fun isSafeAddonPath(path: String): Boolean {

        if (path.isBlank() || path.length > 200) {
            return false
        }

        if (path.startsWith("/") ||
            path.startsWith("\\") ||
            path.contains("\\") ||
            path.contains(":")
        ) {
            return false
        }

        val segments = path.split('/')

        if (segments.any {
                it.isBlank() || it == "." || it == ".."
            }) {
            return false
        }

        if (segments.first() !in setOf(
                "behavior_pack",
                "resource_pack"
            )
        ) {
            return false
        }

        return true
    }
}
