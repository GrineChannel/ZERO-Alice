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
        "models/gemma-4-E2B-it.litertlm"
    )

    // ИНИЦИАЛИЗАЦИЯ МОДЕЛИ

    fun initialize(
        onReady: () -> Unit,
        onError: (String) -> Unit
    ) {
        executor.execute {
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

                val newEngine = Engine(config)

                newEngine.initialize()

                val newConversation =
                    newEngine.createConversation(
                        ConversationConfig(
                            systemInstruction = Contents.of(
                                """
                                Тебя зовут Элис.
                                Ты персональный ИИ-помощник ZERO Project.
                                Ты дружелюбная, энергичная, эмоциональная
                                и внимательная собеседница.
                                Общайся на русском языке,
                                если пользователь не попросил другой язык.
                                Отвечай естественно и по существу.
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

                mainHandler.post {
                    onError(
                        error.message ?: "Не удалось загрузить модель"
                    )
                }
            }
        }
    }

    // ОТПРАВКА СООБЩЕНИЯ

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

    // СОСТОЯНИЕ ДВИЖКА

    fun isReady(): Boolean {
        return initialized
    }

    // ОСВОБОЖДЕНИЕ РЕСУРСОВ

    fun close() {
        executor.execute {

            try {
                conversation?.close()
            } catch (_: Throwable) {
            }

            try {
                engine?.close()
            } catch (_: Throwable) {
            }

            conversation = null
            engine = null
            initialized = false
        }

        executor.shutdown()
    }
}