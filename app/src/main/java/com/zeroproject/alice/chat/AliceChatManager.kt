
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

    private val memoryManager = AliceMemoryManager(
        memory,
        engine
    )

    @Volatile
    private var contextInjected = false

    private fun isExcludedFromContext(content: String): Boolean {
        return memory.getForgottenTerms().any { term ->
            if (term.startsWith("key:")) {
                // Key exclusions are checked against saved fact keys separately.
                false
            } else {
                content.contains(term, ignoreCase = true)
            }
        }
    }

    // Memory control commands are UI actions, not dialogue for the model.
    // Older versions persisted them; exclude those old entries too.
    private fun isMemoryControlMessage(content: String): Boolean {
        val text = content.trim().lowercase()
        return listOf(
            "забудь ", "забудь всё", "забудь все",
            "удали из памяти ", "очисти память",
            "запомни ", "запомни,", "запомни:",
            "сохрани ", "сохрани,", "сохрани:"
        ).any { text.startsWith(it) }
    }

    private fun buildPreviousContext(): String {

        val forgottenValues = memory.getForgottenTerms()
            .filterNot { it.startsWith("key:") }

        // Old app versions stored memory commands and their replies as
        // adjacent chat records. Skip both, without deleting user history.
        var skipNextAssistant = false
        val messages = memory.getMessages(5000).filter { message ->
            if (message.role == "user") {
                skipNextAssistant = isMemoryControlMessage(message.content)
                !skipNextAssistant
            } else if (message.role == "assistant" && skipNextAssistant) {
                skipNextAssistant = false
                false
            } else {
                skipNextAssistant = false
                true
            }
        }.filterNot { message ->
            forgottenValues.any { term ->
                message.content.contains(term, ignoreCase = true)
            }
        }.takeLast(30)

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

        val forgottenKeys = memory.getForgottenTerms()
            .filter { it.startsWith("key:") }
            .map { it.removePrefix("key:") }
        val facts = memory.getFacts().filterNot { fact ->
            forgottenKeys.any { it.equals(fact.key, ignoreCase = true) } ||
                isExcludedFromContext(fact.key + " " + fact.value)
        }

        if (facts.isEmpty()) return ""

        val savedFacts = facts.takeLast(50).joinToString("\n") {
            "- ${it.key}: ${it.value.take(300)}"
        }

        return """
            ДОЛГОВРЕМЕННАЯ ПАМЯТЬ:

            Используй эти сведения, если они относятся
            к текущему разговору.

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
        onError: (String) -> Unit,
        onHistoryChanged: (() -> Unit)? = null
    ) {

        if (text.isBlank()) {
            onError("Сообщение не может быть пустым")
            return
        }

        // Сначала проверяем команды управления памятью.

        val commandResponse = memoryManager.executeMemoryCommand {
            memoryCommands.handle(text)
        }

        if (commandResponse != null) {

            // The memory command and cancellation were serialized with
            // background fact writes inside AliceMemoryManager.

            // Notify the UI to reload the persisted history after changes.
            onHistoryChanged?.invoke()

            // Do not persist commands or their replies: doing so would
            // reintroduce forgotten values after a reset or restart.
            contextInjected = false

            // Memory changes must also invalidate Gemma's in-session chat.
            // Only report success once the old conversation has been replaced.
            if (engine.isReady()) {
                engine.resetChatConversation(
                    onComplete = { onResponse(commandResponse) },
                    onError = { error ->
                        onError("Память обновлена, но контекст ИИ не сброшен: $error")
                    }
                )
            } else {
                onResponse(commandResponse)
            }
            return
        }

        if (!engine.isReady()) {
            onError("Локальный ИИ ещё не загружен")
            return
        }

        // Формируем контекст для основной беседы.

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

                // После ответа отправляем обе стороны
                // диалога на отдельный анализ памяти.

                val dialogue = """
                    Пользователь:
                    $text

                    Элис:
                    $response
                """.trimIndent()

                memoryManager.analyzeDialogue(
                    dialogue = dialogue
                )

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
