
package com.zeroproject.alice.chat

import com.zeroproject.alice.AliceLocalEngine
import com.zeroproject.alice.memory.AliceMemory

class AliceChatManager(
    private val engine: AliceLocalEngine,
    private val memory: AliceMemory
) {

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
            Ниже приведена история нашего предыдущего разговора.
            Используй её для восстановления контекста и понимания
            предыдущих сообщений.

            Не отвечай на старые сообщения повторно.
            Ответь только на новое сообщение пользователя.

            ИСТОРИЯ:
            $transcript

            КОНЕЦ ИСТОРИИ.
            
            Новое сообщение пользователя:
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

        if (!engine.isReady()) {
            onError("Локальный ИИ ещё не загружен")
            return
        }

        val prompt = if (contextInjected) {
            text
        } else {
            buildPreviousContext() + text
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
