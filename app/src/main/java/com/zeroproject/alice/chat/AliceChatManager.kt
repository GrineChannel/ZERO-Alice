
package com.zeroproject.alice.chat

import com.zeroproject.alice.AliceLocalEngine
import com.zeroproject.alice.memory.AliceMemory
import com.zeroproject.alice.memory.AliceMemoryCommands

class AliceChatManager(
    private val engine: AliceLocalEngine,
    private val memory: AliceMemory
) {

    private val memoryCommands = AliceMemoryCommands(memory)

    @Volatile
    private var contextInjected = false

    private fun buildPreviousContext(): String {
        val messages = memory.getMessages(5000)
            .takeLast(30)

        if (messages.isEmpty()) return ""

        val transcript = messages.joinToString("\n") { message ->
            val role = when (message.role) {
                "user" -> "Пользователь"
                "assistant" -> "Элис"
                else -> message.role
            }

            val content = message.content.take(1500)

            "$role: $content"
        }

        return """
            ИСТОРИЯ ПРЕДЫДУЩЕГО РАЗГОВОРА:

            $transcript

            КОНЕЦ ИСТОРИИ.
        """.trimIndent()
    }

    private fun buildMemoryContext(): String {
        val facts = memory.getFacts()

        if (facts.isEmpty()) return ""

        val savedFacts = facts
            .takeLast(50)
            .joinToString("\n") {
                "- ${it.key}: ${it.value.take(300)}"
            }

        return """
            ДОЛГОВРЕМЕННАЯ ПАМЯТЬ:

            Ниже находятся сведения, которые пользователь
            явно попросил сохранить.

            Используй их, когда они относятся к разговору.
            Не выдумывай дополнительные факты.
            Если сведения противоречат друг другу,
            уточни информацию у пользователя.

            $savedFacts

            КОНЕЦ ДОЛГОВРЕМЕННОЙ ПАМЯТИ.
        """.trimIndent()
    }

    fun sendMessage(
        text: String,
        onResponse: (String) -> Unit,
        onError: (String) -> Unit
    ) {

        if (text.isBlank()) {
            onError("Сообщение не может быть пустым")
            return
        }

        // Сначала проверяем команды памяти.
        // Они работают даже без загруженной Gemma.

        val commandResponse = memoryCommands.handle(text)

        if (commandResponse != null) {
            memory.saveMessage("user", text)
            memory.saveMessage("assistant", commandResponse)

            onResponse(commandResponse)
            return
        }

        if (!engine.isReady()) {
            onError("Локальный ИИ ещё не загружен")
            return
        }

        val previousContext = if (contextInjected) {
            ""
        } else {
            buildPreviousContext()
        }

        // Долговременная память добавляется
        // к каждому новому сообщению.

        val factsContext = buildMemoryContext()

        val prompt = buildString {

            if (previousContext.isNotBlank()) {
                append(previousContext)
                append("\n\n")
            }

            if (factsContext.isNotBlank()) {
                append(factsContext)
                append("\n\n")
            }

            append("Новое сообщение пользователя:\n")
            append(text)
        }

        memory.saveMessage("user", text)

        engine.sendMessage(
            text = prompt,

            onResponse = { response ->

                memory.saveMessage("assistant", response)

                contextInjected = true

                onResponse(response)
            },

            onError = { error ->
                onError(error)
            }
        )
    }

    fun getHistory(): List<ChatMessage> {
        return memory.getMessages(5000).map {
            ChatMessage(
                id = it.id,
                role = it.role,
                content = it.content,
                timestamp = it.timestamp
            )
        }
    }

    fun clearHistory() {
        memory.clearHistory()
        contextInjected = false
    }

    fun resetContext() {
        contextInjected = false
    }
}

data class ChatMessage(
    val id: Long,
    val role: String,
    val content: String,
    val timestamp: Long
)
