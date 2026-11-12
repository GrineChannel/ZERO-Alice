package com.zeroproject.alice.files

import android.content.Context
import java.io.File
import java.security.MessageDigest

/**
 * Explicit edits with one backup per workspace file and conflict-aware undo.
 * Backups live outside the user-editable workspace, in app-private storage.
 */
class AliceFileRevision(
    context: Context,
    private val workspace: AliceWorkspace
) {
    private val backups = File(context.filesDir, "alice_revision_backups")
    private val lock = Any()

    private fun digest(text: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(text.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

    private fun key(path: String): String {
        val normalized = path.trim().replace('\\', '/')
        require(normalized.isNotBlank()) { "Путь не указан" }
        // Resolve through workspace before using path as a backup key.
        workspace.readText(normalized)
        return digest(normalized)
    }

    fun preview(path: String, replacement: String): String {
        val original = workspace.readText(path)
        require(replacement.toByteArray(Charsets.UTF_8).size <= 1024 * 1024) {
            "Новый файл слишком большой"
        }
        return "Файл: $path\nСейчас: ${original.length} символов\nПосле замены: ${replacement.length} символов\nИзменение: полная замена текста. Для применения нужна отдельная команда."
    }

    fun replace(path: String, replacement: String): String = synchronized(lock) {
        val original = workspace.readText(path)
        require(replacement.toByteArray(Charsets.UTF_8).size <= 1024 * 1024) {
            "Новый файл слишком большой"
        }
        require(backups.isDirectory || backups.mkdirs()) { "Не удалось создать папку резервных копий" }
        val id = key(path)
        val backup = File(backups, "$id.original")
        val expected = File(backups, "$id.expected")
        // Do not discard an un-restored backup from an earlier edit.
        require(!backup.exists() && !expected.exists()) {
            "Для этого файла уже есть резервная копия. Сначала выполни откат."
        }
        backup.writeText(original, Charsets.UTF_8)
        try {
            workspace.writeText(path, replacement, overwrite = true)
            expected.writeText(digest(replacement), Charsets.UTF_8)
        } catch (error: Exception) {
            workspace.writeText(path, original, overwrite = true)
            backup.delete()
            expected.delete()
            throw error
        }
        "Файл обновлён: $path. Резервная копия сохранена."
    }

    fun undo(path: String): String = synchronized(lock) {
        val id = key(path)
        val backup = File(backups, "$id.original")
        val expected = File(backups, "$id.expected")
        require(backup.isFile && expected.isFile) { "Резервная копия не найдена" }
        val current = workspace.readText(path)
        require(digest(current) == expected.readText(Charsets.UTF_8)) {
            "Файл изменился после замены. Автоматический откат остановлен."
        }
        workspace.writeText(path, backup.readText(Charsets.UTF_8), overwrite = true)
        backup.delete()
        expected.delete()
        "Исходная версия восстановлена: $path"
    }
}
