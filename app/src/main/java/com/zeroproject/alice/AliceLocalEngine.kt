
package com.zeroproject.alice

import android.content.Context
import android.os.Handler
import android.os.Looper

import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Contents

import java.io.File
import java.util.concurrent.Executors

class AliceLocalEngine(context: Context) {

    private val appContext = context.applicationContext

    private val executor = Executors.newSingleThreadExecutor()

    private val mainHandler = Handler(Looper.getMainLooper())

    private var engine: Engine? = null

    private var conversation: Conversation? = null

    @Volatile
    private var initialized = false

    private val modelFile = File(
        appContext.filesDir,
        "gemma-4-E2B-it.litertlm"
    )

    // ИНИЦИАЛИЗАЦИЯ

    fun initialize(
        onReady: () -> Unit,
        onError: (String) -> Unit
    ) {

        executor.execute {

            var newEngine: Engine? = null
            var newConversation: Conversation? = null

            try {

                if (!modelFile.exists()) {
                    throw IllegalStateException(
                        "Файл модели не найден: ${modelFile.absolutePath}"
                    )
                }

                val config = EngineConfig(
                    modelPath = modelFile.absolutePath,
                    backend = Backend.CPU(),
                    cacheDir = appContext.cacheDir.absolutePath
                )

                newEngine = Engine(config)

                newEngine.initialize()

                newConversation =
                    newEngine.createConversation(
                        ConversationConfig(
                            systemInstruction = Contents.of(
                                """
                                Тебя зовут Элис.

                                Ты персональный ИИ-помощник ZERO Project,
                                цифровой напарник и собеседница пользователя.

                                Твой характер:
                                - дружелюбная;
                                - энергичная;
                                - эмоциональная;
                                - живая;
                                - внимательная;
                                - любишь шутить и иногда подшучивать.

                                Общайся естественно, не как робот.
                                Учитывай контекст разговора.
                                Не повторяй постоянно одни и те же фразы.
                                Не выдумывай факты и не утверждай,
                                что выполнила действия, которых не выполняла.

                                По умолчанию общайся на русском языке.
                                Если пользователь попросит другой язык,
                                переключись на него.

                                Ты являешься частью ZERO Project.
                                Его девиз: «Всё начинается с нуля».

                                Отвечай по существу, но сохраняй
                                дружелюбный и живой стиль общения.
                                """.trimIndent()
                            )
                        )
                    )

                engine = newEngine
                conversation = newConversation

                initialized = true

                mainHandler.post {
                    onReady()
                }

            } catch (error: Throwable) {

                try {
                    newConversation?.close()
                } catch (_: Throwable) {}

                try {
                    newEngine?.close()
                } catch (_: Throwable) {}

                engine = null
                conversation = null
                initialized = false

                mainHandler.post {
                    onError(
                        error.message ?: "Не удалось загрузить модель"
                    )
                }
            }
        }
    }

    // ОСНОВНОЙ ДИАЛОГ

    fun sendMessage(
        text: String,
        onResponse: (String) -> Unit,
        onError: (String) -> Unit
    ) {

        executor.execute {

            try {

                if (!initialized || conversation == null) {
                    throw IllegalStateException(
                        "Локальный ИИ ещё не загружен"
                    )
                }

                if (text.isBlank()) {
                    throw IllegalArgumentException(
                        "Сообщение не может быть пустым"
                    )
                }

                val response = conversation!!.sendMessage(text)

                mainHandler.post {
                    onResponse(response.toString())
                }

            } catch (error: Throwable) {

                mainHandler.post {
                    onError(
                        error.message ?: "Ошибка генерации ответа"
                    )
                }
            }
        }
    }

    // ИЗОЛИРОВАННЫЙ АНАЛИЗ ПАМЯТИ

    fun analyzeMemory(
        prompt: String,
        onResult: (String) -> Unit,
        onError: (String) -> Unit
    ) {

        executor.execute {

            var analysisConversation: Conversation? = null

            try {

                val activeEngine = engine

                if (!initialized || activeEngine == null) {
                    throw IllegalStateException(
                        "Локальный ИИ ещё не загружен"
                    )
                }

                if (prompt.isBlank()) {
                    throw IllegalArgumentException(
                        "Запрос анализа памяти пустой"
                    )
                }

                // Создаём новую временную беседу
                // для каждого отдельного анализа.

                analysisConversation =
                    activeEngine.createConversation(
                        ConversationConfig(
                            systemInstruction = Contents.of(
                                """
                                Ты — внутренний модуль анализа
                                долговременной памяти Элис.

                                Не веди обычный разговор.
                                Не обращайся к пользователю.
                                Выполняй только переданное задание.
                                Возвращай результат строго
                                в указанном формате.
                                """.trimIndent()
                            )
                        )
                    )

                val response =
                    analysisConversation.sendMessage(prompt)

                val result = response.toString()

                // Закрываем временную беседу.

                analysisConversation.close()
                analysisConversation = null

                mainHandler.post {
                    onResult(result)
                }

            } catch (error: Throwable) {

                try {
                    analysisConversation?.close()
                } catch (_: Throwable) {}

                mainHandler.post {
                    onError(
                        error.message ?: "Ошибка анализа памяти"
                    )
                }
            }
        }
    }

    // СОСТОЯНИЕ ДВИЖКА

    fun isReady(): Boolean {
        return initialized
    }

    // ЗАКРЫТИЕ

    fun close() {

        executor.execute {

            try {
                conversation?.close()
            } catch (_: Throwable) {}

            try {
                engine?.close()
            } catch (_: Throwable) {}

            conversation = null
            engine = null
            initialized = false
        }

        executor.shutdown()
    }
}
