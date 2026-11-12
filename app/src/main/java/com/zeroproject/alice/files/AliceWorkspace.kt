package com.zeroproject.alice.files

import android.content.Context
import java.io.File
import java.io.IOException

/**
 * App-private workspace. Never resolves paths outside this directory.
 * File operations are synchronous; callers should use a background thread
 * for large files.
 */
class AliceWorkspace(context: Context) {
    private val root = File(context.filesDir, "alice_workspace").canonicalFile

    init {
        if (!root.isDirectory && !root.mkdirs()) {
            throw IOException("Не удалось создать рабочую папку Элис")
        }
    }

    data class Entry(
        val path: String,
        val directory: Boolean,
        val sizeBytes: Long
    )

    fun rootPath(): String = root.absolutePath

    private fun resolve(relativePath: String): File {
        val name = relativePath.trim().replace('\\', '/')
        if (name.isEmpty() || name.startsWith('/')) {
            throw IllegalArgumentException("Требуется относительный путь")
        }
        val target = File(root, name).canonicalFile
        if (target == root || !target.toPath().startsWith(root.toPath())) {
            throw SecurityException("Путь выходит за пределы рабочей папки")
        }
        return target
    }

    fun list(relativeDirectory: String = ""): List<Entry> {
        val directory = if (relativeDirectory.isBlank()) root else resolve(relativeDirectory)
        if (!directory.isDirectory) return emptyList()
        return directory.listFiles().orEmpty().map {
            Entry(root.toPath().relativize(it.toPath()).toString(), it.isDirectory, it.length())
        }.sortedWith(compareByDescending<Entry> { it.directory }.thenBy { it.path })
    }

    fun readText(path: String, maxBytes: Long = 1024L * 1024L): String {
        val file = resolve(path)
        require(file.isFile) { "Файл не найден" }
        require(file.length() <= maxBytes) { "Файл слишком большой" }
        return file.readText(Charsets.UTF_8)
    }

    fun writeText(path: String, content: String, overwrite: Boolean = false): Entry {
        val file = resolve(path)
        require(overwrite || !file.exists()) { "Файл уже существует" }
        require(content.toByteArray(Charsets.UTF_8).size <= 1024 * 1024) {
            "Превышен лимит размера текстового файла"
        }
        val parent = file.parentFile ?: throw IOException("Нет родительской папки")
        if (!parent.isDirectory && !parent.mkdirs()) {
            throw IOException("Не удалось создать папку")
        }
        file.writeText(content, Charsets.UTF_8)
        return Entry(path, false, file.length())
    }

    fun createDirectory(path: String): Boolean {
        val directory = resolve(path)
        return directory.isDirectory || directory.mkdirs()
    }

    fun delete(path: String): Boolean {
        val file = resolve(path)
        // No recursive deletion: removing a project tree requires a separate,
        // explicit operation with appropriate confirmation.
        return file.exists() && file.delete()
    }
}
