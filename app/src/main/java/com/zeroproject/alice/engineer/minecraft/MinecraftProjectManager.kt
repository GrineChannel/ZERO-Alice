
package com.zeroproject.alice.engineer.minecraft

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class MinecraftProject(
    val id: String,
    val name: String,
    val description: String,
    val directory: File,
    val createdAt: Long
)

class MinecraftProjectManager(context: Context) {

    private val appContext = context.applicationContext

    private val projectsDirectory = File(
        appContext.filesDir,
        "minecraft_projects"
    )

    init {
        if (!projectsDirectory.exists()) {
            projectsDirectory.mkdirs()
        }
    }

    @Synchronized
    fun createProject(
        name: String,
        description: String = ""
    ): MinecraftProject {
        val cleanName = name.trim()

        require(cleanName.isNotBlank()) {
            "Название проекта не может быть пустым"
        }

        require(cleanName.length <= 100) {
            "Слишком длинное название проекта"
        }

        val id = UUID.randomUUID().toString()
        val directory = File(projectsDirectory, id)

        check(directory.mkdirs()) {
            "Не удалось создать папку проекта"
        }

        val createdAt = System.currentTimeMillis()

        val metadata = JSONObject().apply {
            put("id", id)
            put("name", cleanName)
            put("description", description)
            put("createdAt", createdAt)
            put("type", "minecraft_bedrock_addon")
        }

        try {
            File(directory, "project.json").writeText(
                metadata.toString(4),
                Charsets.UTF_8
            )

            File(directory, "behavior_pack").mkdirs()
            File(directory, "resource_pack").mkdirs()
            File(directory, "backups").mkdirs()

        } catch (error: Throwable) {
            directory.deleteRecursively()
            throw error
        }

        return MinecraftProject(
            id = id,
            name = cleanName,
            description = description,
            directory = directory,
            createdAt = createdAt
        )
    }

    @Synchronized
    fun listProjects(): List<MinecraftProject> {
        if (!projectsDirectory.exists()) {
            return emptyList()
        }

        return projectsDirectory.listFiles()
            ?.filter { it.isDirectory }
            ?.mapNotNull { readProjectMetadata(it) }
            ?.sortedByDescending { it.createdAt }
            ?: emptyList()
    }

    @Synchronized
    fun getProject(id: String): MinecraftProject? {
        if (!isValidId(id)) return null

        val directory = File(projectsDirectory, id)

        if (!directory.exists() || !directory.isDirectory) {
            return null
        }

        return readProjectMetadata(directory)
    }

    /**
     * Read-only inventory before retrying code generation.
     * Never follows paths outside the project's canonical root.
     */
    @Synchronized
    fun inspectProjectFiles(projectId: String): String {
        val project = requireProject(projectId)
        val root = project.directory.canonicalFile
        val files = mutableListOf<String>()
        var totalBytes = 0L
        for (pack in listOf("behavior_pack", "resource_pack")) {
            val directory = File(root, pack)
            if (!directory.isDirectory) continue
            directory.walkTopDown().onEnter { folder ->
                val safe = folder.canonicalPath.startsWith(root.path + File.separator)
                safe && !java.nio.file.Files.isSymbolicLink(folder.toPath())
            }.filter { it.isFile && !java.nio.file.Files.isSymbolicLink(it.toPath()) }
                .take(201).forEach { file ->
                    val canonical = file.canonicalFile
                    if (canonical.path.startsWith(root.path + File.separator)) {
                        if (files.size < 200) {
                            files.add("${canonical.relativeTo(root).invariantSeparatorsPath} (${canonical.length()} байт)")
                        }
                        totalBytes += canonical.length()
                    }
                }
        }
        return buildString {
            append("Проект: ${project.name} (${project.id.take(8)})\\n")
            append("Файлов (до 200): ${files.size}, размер: $totalBytes байт\\n")
            if (files.isEmpty()) append("Файлы дополнения пока не созданы.")
            else append(files.sorted().joinToString("\\n"))
            if (files.size >= 200) append("\\nСписок ограничен 200 файлами.")
            append("\\nПроверка только читает файлы и ничего не изменяет.")
        }
    }

    /**
     * Read-only list of staged generation batches. Never scans outside backups/previews.
     */
    @Synchronized
    fun listPreviewBatches(projectId: String): String {
        val project = requireProject(projectId)
        val root = project.directory.canonicalFile
        val previews = File(root, "backups/previews").canonicalFile
        require(previews.path.startsWith(root.path + File.separator)) {
            "Недопустимая папка предварительных версий"
        }
        if (!previews.isDirectory) return "Предварительных версий пока нет."
        val batches = previews.listFiles()
            ?.filter { it.isDirectory && !java.nio.file.Files.isSymbolicLink(it.toPath()) }
            ?.sortedByDescending { it.lastModified() }
            ?.take(20)
            .orEmpty()
        if (batches.isEmpty()) return "Предварительных версий пока нет."
        return buildString {
            append("Предварительные версии проекта ${project.name}:\n")
            for (batch in batches) {
                val safeBatch = batch.canonicalFile
                if (!safeBatch.path.startsWith(previews.path + File.separator)) continue
                val sample = safeBatch.walkTopDown().onEnter {
                    it.canonicalPath.startsWith(safeBatch.path) &&
                        !java.nio.file.Files.isSymbolicLink(it.toPath())
                }.filter { it.isFile && !java.nio.file.Files.isSymbolicLink(it.toPath()) }
                    .take(31).toList()
                append("${batch.name.take(8)} | файлов: ${sample.size.coerceAtMost(30)}")
                if (sample.size > 30) append("+")
                append("\n")
            }
            append("Основные файлы не изменены.")
        }
    }

    @Synchronized
    fun writeFile(
        projectId: String,
        relativePath: String,
        content: String
    ) {
        val project = requireProject(projectId)
        val target = resolveSafePath(
            project.directory,
            relativePath
        )

        check(target != project.directory) {
            "Нельзя перезаписать папку проекта"
        }

        target.parentFile?.mkdirs()
        target.writeText(content, Charsets.UTF_8)
    }

    @Synchronized
    fun readFile(
        projectId: String,
        relativePath: String
    ): String {
        val project = requireProject(projectId)
        val target = resolveSafePath(
            project.directory,
            relativePath
        )

        check(target.isFile) {
            "Файл не найден"
        }

        return target.readText(Charsets.UTF_8)
    }

    @Synchronized
    fun deleteFile(
        projectId: String,
        relativePath: String
    ) {
        val project = requireProject(projectId)
        val target = resolveSafePath(
            project.directory,
            relativePath
        )

        check(target != project.directory) {
            "Нельзя удалить корневую папку проекта"
        }

        check(target.exists()) {
            "Файл не найден"
        }

        check(target.isFile) {
            "Удаление папок пока не поддерживается"
        }

        check(target.delete()) {
            "Не удалось удалить файл"
        }
    }

    private fun requireProject(id: String): MinecraftProject {
        return getProject(id)
            ?: throw IllegalArgumentException(
                "Проект не найден: $id"
            )
    }

    private fun readProjectMetadata(
        directory: File
    ): MinecraftProject? {
        return try {
            val metadataFile = File(directory, "project.json")

            if (!metadataFile.isFile) return null

            val json = JSONObject(
                metadataFile.readText(Charsets.UTF_8)
            )

            MinecraftProject(
                id = json.getString("id"),
                name = json.getString("name"),
                description = json.optString("description"),
                directory = directory,
                createdAt = json.optLong("createdAt")
            )
        } catch (_: Throwable) {
            null
        }
    }

    private fun resolveSafePath(
        projectDirectory: File,
        relativePath: String
    ): File {
        require(relativePath.isNotBlank()) {
            "Путь к файлу не может быть пустым"
        }

        val root = projectDirectory.canonicalFile
        val target = File(root, relativePath).canonicalFile

        check(
            target.path == root.path ||
            target.path.startsWith(root.path + File.separator)
        ) {
            "Недопустимый путь за пределами проекта"
        }

        return target
    }

    private fun isValidId(id: String): Boolean {
        return try {
            UUID.fromString(id)
            true
        } catch (_: IllegalArgumentException) {
            false
        }
    }
}
