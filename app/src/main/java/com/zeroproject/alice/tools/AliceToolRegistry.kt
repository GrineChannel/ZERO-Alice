package com.zeroproject.alice.tools

import com.zeroproject.alice.device.AliceDeviceActions
import com.zeroproject.alice.files.AliceWorkspace
import com.zeroproject.alice.tasks.AliceTaskCommands

/**
 * Only explicit user commands can invoke tools. Model output is never
 * treated as an instruction. File access is limited to the private workspace.
 */
class AliceToolRegistry(
    private val deviceActions: AliceDeviceActions,
    private val workspace: AliceWorkspace? = null,
    private val taskCommands: AliceTaskCommands? = null
) {
    data class Tool(
        val id: String,
        val title: String,
        val category: String
    )

    data class Result(
        val handled: Boolean,
        val succeeded: Boolean,
        val message: String
    )

    fun availableTools(): List<Tool> =
        deviceActions.availableActions().map {
            Tool(it.id, it.title, "device")
        } + listOf(
            Tool("workspace_list", "Показать файлы Элис", "files"),
            Tool("workspace_mkdir", "Создать папку Элис", "files"),
            Tool("workspace_write", "Создать текстовый файл Элис", "files"),
            Tool("workspace_read", "Прочитать файл Элис", "files")
        )

    fun executeUserCommand(text: String): Result {
        taskCommands?.handle(text)?.let {
            return Result(true, true, it)
        }
        val command = text.trim()
        val lower = command.lowercase()

        if (lower == "покажи файлы элис" || lower == "список файлов элис") {
            return withWorkspace { space ->
                val entries = space.list()
                if (entries.isEmpty()) {
                    "Рабочая папка Элис пока пуста."
                } else {
                    "Файлы Элис:\n" + entries.take(50).joinToString("\n") {
                        (if (it.directory) "📁 " else "📄 ") + it.path
                    }
                }
            }
        }

        if (lower.startsWith("создай папку элис ")) {
            val path = command.substring("создай папку элис ".length).trim()
            return withWorkspace { space ->
                if (space.createDirectory(path)) "Папка создана: $path"
                else "Не удалось создать папку: $path"
            }
        }

        if (lower.startsWith("прочитай файл элис ")) {
            val path = command.substring("прочитай файл элис ".length).trim()
            return withWorkspace { space ->
                "Содержимое $path:\n" + space.readText(path).take(4000)
            }
        }

        if (lower.startsWith("создай файл элис ")) {
            val argument = command.substring("создай файл элис ".length)
            val separator = argument.indexOf(" :: ")
            if (separator < 1) {
                return Result(true, false, "Формат: создай файл элис путь/имя.txt :: текст")
            }
            val path = argument.substring(0, separator).trim()
            val content = argument.substring(separator + 4)
            return withWorkspace { space ->
                space.writeText(path, content)
                "Файл создан: $path"
            }
        }

        val id = deviceActions.resolveCommand(text)
            ?: return Result(false, false, "")
        val tool = availableTools().firstOrNull { it.id == id }
            ?: return Result(false, false, "")
        val success = deviceActions.execute(tool.id)
        return Result(
            handled = true,
            succeeded = success,
            message = if (success) "Открываю: ${tool.title}."
            else "Не удалось выполнить: ${tool.title}."
        )
    }

    private inline fun withWorkspace(action: (AliceWorkspace) -> String): Result {
        val space = workspace ?: return Result(true, false, "Файловый модуль недоступен")
        return try {
            Result(true, true, action(space))
        } catch (error: Exception) {
            Result(true, false, "Ошибка работы с файлами: ${error.message}")
        }
    }
}
