
package com.zeroproject.alice.chat

import com.zeroproject.alice.AliceLocalEngine
import com.zeroproject.alice.memory.AliceMemory
import com.zeroproject.alice.memory.AliceMemoryCommands
import com.zeroproject.alice.memory.AliceMemoryManager

class AliceChatManager(
    private val engine: AliceLocalEngine,
    private val memory: AliceMemory
) {

    private val memoryCommands = AliceMemoryCommands(memory)

    private val memoryManager = AliceMemoryManager(memory)

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

        val savedFacts = facts.takeLast(50).joinToString("\n") {
            "- ${it.key}: ${it.value.take(300)}"
        }

        return """
            ДОЛГОВРЕМЕННАЯ ПАМЯТЬ:

            Ниже находятся сведения, которые были сохранены
            автоматически или по просьбе пользователя.

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

        // Обработка команд управления памятью.

        val commandResponse = memoryCommands.handle(text)

        if (commandResponse != null) {

            memory.saveMessage("user", text)
            memory.saveMessage("assistant", commandResponse)

            onResponse(commandResponse)
            return
        }

        // Автоматическое сохранение полезной информации.

        try {
            memoryManager.analyzeAndSave(text)
        } catch (_: Throwable) {
            // Ошибка памяти не должна мешать общению.
        }

        if (!engine.isReady()) {
            onError("Локальный ИИ ещё не загружен")
            return
        }

        val previousContext =
            if (contextInjected) "" else buildPreviousContext()

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
                it.id,
                it.role,
                it.content,
                it.timestamp
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
