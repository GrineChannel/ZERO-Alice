
package com.zeroproject.alice

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.os.Build
import android.view.WindowManager
import android.widget.Toast

import com.zeroproject.alice.screens.AliceHomeScreen
import com.zeroproject.alice.screens.AliceModelScreen
import com.zeroproject.alice.screens.AliceChatScreen

import com.zeroproject.alice.memory.AliceMemory
import com.zeroproject.alice.chat.AliceChatManager

import java.io.File

class MainActivity : Activity() {

    private lateinit var modelDownloader: AliceModelDownloader
    private lateinit var localEngine: AliceLocalEngine

    private lateinit var aliceMemory: AliceMemory
    private lateinit var chatManager: AliceChatManager

    private var modelScreen: AliceModelScreen? = null
    private var chatScreen: AliceChatScreen? = null

    private val chatHistory = mutableListOf<Pair<String, Boolean>>()

    private var isChatOpen = false
    private var isModelScreenOpen = false
    private var isDownloading = false

    private var isInitializingAI = false
    private var isGenerating = false
    private var engineInitializationStarted = false

    private var lastBackPressedAt = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        modelDownloader = AliceModelDownloader(this)
        localEngine = AliceLocalEngine(this)

        aliceMemory = AliceMemory(this)
        chatManager = AliceChatManager(
            localEngine,
            aliceMemory
        )

        window.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
        )

        showHome()
    }

    override fun onDestroy() {

        if (::modelDownloader.isInitialized) {
            modelDownloader.close()
        }

        if (::localEngine.isInitialized) {
            localEngine.close()
        }

        if (::aliceMemory.isInitialized) {
            aliceMemory.close()
        }

        super.onDestroy()
    }

    // СИСТЕМНАЯ КНОПКА НАЗАД

    @Deprecated("Deprecated in Android API")
    override fun onBackPressed() {

        if (isChatOpen || isModelScreenOpen) {
            showHome()
            return
        }

        val currentTime = System.currentTimeMillis()

        if (currentTime - lastBackPressedAt < 2000) {
            finish()
        } else {
            lastBackPressedAt = currentTime

            Toast.makeText(
                this,
                "Нажмите ещё раз, чтобы выйти из ZERO: Alice",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // ГЛАВНЫЙ ЭКРАН

    private fun showHome() {

        isChatOpen = false
        isModelScreenOpen = false

        chatScreen = null
        modelScreen = null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(true)
        }

        val homeScreen = AliceHomeScreen(
            context = this,

            onChatClick = {
                showChat()
            },

            onVoiceClick = {
                showMessage(
                    "Голосовой модуль будет добавлен позже"
                )
            },

            onModelClick = {
                showModelScreen()
            },

            onSettingsClick = {
                showMessage(
                    "Настройки будут добавлены следующим этапом"
                )
            }
        )

        setContentView(homeScreen.create())
    }

    // ЭКРАН МОДЕЛИ ИИ

    private fun showModelScreen() {

        isChatOpen = false
        isModelScreenOpen = true
        chatScreen = null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(true)
        }

        val modelFile = File(
            filesDir,
            "gemma-4-E2B-it.litertlm"
        )

        val modelExists = modelDownloader.isModelDownloaded()

        val screen = AliceModelScreen(
            context = this,
            isModelDownloaded = modelExists,
            isDownloading = isDownloading,

            modelSize = if (modelExists) {
                formatBytes(modelFile.length())
            } else {
                "0 МБ / 0 МБ"
            },

            onDownloadClick = {
                startModelDownload()
            },

            onCancelClick = {
                cancelModelDownload()
            },

            onBackClick = {
                showHome()
            }
        )

        modelScreen = screen

        setContentView(screen.create())
    }

    // ЗАПУСК ЗАГРУЗКИ

    private fun startModelDownload() {

        if (isDownloading) return

        isDownloading = true

        modelScreen?.updateAction(
            "ОТМЕНИТЬ ЗАГРУЗКУ",
            true
        )

        modelScreen?.updateStatus(
            "Подключение к серверу..."
        )

        modelDownloader.start(
            object : AliceModelDownloader.Listener {

                override fun onProgress(
                    downloaded: Long,
                    total: Long,
                    percent: Int
                ) {

                    if (!isModelScreenOpen) return

                    modelScreen?.updateProgress(percent)

                    modelScreen?.updateStatus(
                        "Загрузка модели: $percent%"
                    )

                    modelScreen?.updateSize(
                        "${formatBytes(downloaded)} / ${formatBytes(total)}"
                    )
                }

                override fun onComplete(file: File) {

                    isDownloading = false

                    modelScreen?.updateProgress(100)

                    modelScreen?.updateStatus(
                        "Модель успешно загружена и проверена!"
                    )

                    modelScreen?.updateSize(
                        formatBytes(file.length())
                    )

                    modelScreen?.updateAction(
                        "МОДЕЛЬ ЗАГРУЖЕНА",
                        false,
                        0.65f
                    )

                    showMessage(
                        "Gemma 4 E2B успешно загружена!"
                    )
                }

                override fun onError(message: String) {

                    isDownloading = false

                    modelScreen?.updateStatus(
                        "Ошибка загрузки: $message"
                    )

                    modelScreen?.updateAction(
                        "ПОВТОРИТЬ ЗАГРУЗКУ",
                        true
                    )

                    showMessage(message)
                }

                override fun onCancelled() {

                    isDownloading = false

                    modelScreen?.updateStatus(
                        "Загрузка отменена. Можно продолжить позже."
                    )

                    modelScreen?.updateAction(
                        "ПРОДОЛЖИТЬ ЗАГРУЗКУ",
                        true
                    )
                }
            }
        )
    }

    // ОТМЕНА ЗАГРУЗКИ

    private fun cancelModelDownload() {

        if (!isDownloading) return

        modelScreen?.updateStatus(
            "Отмена загрузки..."
        )

        modelDownloader.cancel()
    }

    // ФОРМАТИРОВАНИЕ РАЗМЕРА

    private fun formatBytes(bytes: Long): String {

        val mb = bytes / (1024.0 * 1024.0)

        return if (mb >= 1024) {
            String.format(
                java.util.Locale.US,
                "%.2f ГБ",
                mb / 1024.0
            )
        } else {
            String.format(
                java.util.Locale.US,
                "%.1f МБ",
                mb
            )
        }
    }

    // ЭКРАН ОБЩЕНИЯ

    private fun showChat() {

        isChatOpen = true
        isModelScreenOpen = false
        modelScreen = null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
        }

        // Восстанавливаем историю из SQLite

        if (chatHistory.isEmpty()) {

            val savedMessages = chatManager.getHistory()

            if (savedMessages.isNotEmpty()) {

                for (message in savedMessages) {

                    chatHistory.add(
                        Pair(
                            message.content,
                            message.role == "user"
                        )
                    )
                }

            } else {

                chatHistory.add(
                    Pair(
                        "Привет! Я Элис. ❤️\n\nЯ запускаю свой локальный ИИ. Немного терпения!",
                        false
                    )
                )
            }
        }

        val screen = AliceChatScreen(
            context = this,

            initialMessages = chatHistory,

            initialStatus = getAIStatusText(),

            initialSendEnabled =
                localEngine.isReady() && !isGenerating,

            onBackClick = {
                showHome()
            },

            onSendClick = { message ->
                sendMessage(message)
            }
        )

        chatScreen = screen

        setContentView(screen.create())

        initializeAI()
    }

    // СОСТОЯНИЕ ИИ

    private fun getAIStatusText(): String {

        return when {
            isGenerating -> "●  ЭЛИС ДУМАЕТ..."
            localEngine.isReady() -> "●  ЛОКАЛЬНЫЙ ИИ АКТИВЕН"
            isInitializingAI -> "●  ЗАГРУЗКА ЛОКАЛЬНОГО ИИ..."
            else -> "●  ОЖИДАНИЕ ИИ"
        }
    }

    private fun updateChatControls() {

        if (!isChatOpen) return

        chatScreen?.updateStatus(
            getAIStatusText()
        )

        chatScreen?.setSendEnabled(
            localEngine.isReady() && !isGenerating
        )
    }

    // ИНИЦИАЛИЗАЦИЯ GEMMA

    private fun initializeAI() {

        if (localEngine.isReady()) {
            updateChatControls()
            return
        }

        if (isInitializingAI || engineInitializationStarted) {
            updateChatControls()
            return
        }

        val modelFile = File(
            filesDir,
            "gemma-4-E2B-it.litertlm"
        )

        if (!modelDownloader.isModelDownloaded() || !modelFile.exists()) {

            val text =
                "Я пока не могу запуститься: модель Gemma не найдена. Открой раздел «Модель ИИ» и проверь загрузку."

            chatHistory.add(Pair(text, false))
            chatScreen?.addMessage(text, false)

            updateChatControls()
            return
        }

        isInitializingAI = true
        engineInitializationStarted = true

        updateChatControls()

        localEngine.initialize(

            onReady = {

                isInitializingAI = false

                val text =
                    "Я проснулась! ❤️ Теперь я могу отвечать с помощью локальной Gemma."

                chatHistory.add(Pair(text, false))

                if (isChatOpen) {
                    chatScreen?.addMessage(text, false)
                    updateChatControls()
                }
            },

            onError = { error ->

                isInitializingAI = false
                engineInitializationStarted = false

                val text =
                    "Не удалось запустить локальную модель:\n$error"

                chatHistory.add(Pair(text, false))

                if (isChatOpen) {
                    chatScreen?.addMessage(text, false)

                    chatScreen?.updateStatus(
                        "●  ОШИБКА ЗАПУСКА ИИ"
                    )

                    updateChatControls()
                }

                showMessage("Ошибка запуска Gemma: $error")
            }
        )
    }

    // ОТПРАВКА СООБЩЕНИЯ ЧЕРЕЗ CHAT MANAGER

    private fun sendMessage(message: String) {

        if (message.isBlank()) return

        if (!localEngine.isReady()) {

            showMessage(
                "Элис ещё загружает локальную модель"
            )

            return
        }

        if (isGenerating) return

        // Показываем сообщение пользователя

        chatHistory.add(Pair(message, true))
        chatScreen?.addMessage(message, true)

        // Показываем индикатор генерации

        isGenerating = true

        chatHistory.add(
            Pair("Элис думает...", false)
        )

        chatScreen?.addMessage(
            "Элис думает...",
            false
        )

        updateChatControls()

        // Передаём сообщение менеджеру чата.
        // Он сохраняет переписку и восстанавливает контекст.

        chatManager.sendMessage(

            text = message,

            onResponse = { response ->

                isGenerating = false

                if (chatHistory.isNotEmpty() &&
                    chatHistory.last().first == "Элис думает..."
                ) {
                    chatHistory.removeAt(chatHistory.lastIndex)
                }

                val answer = response.trim().ifEmpty {
                    "Я получила пустой ответ от модели. Попробуй спросить ещё раз."
                }

                chatHistory.add(Pair(answer, false))

                if (isChatOpen) {

                    chatScreen?.setMessages(chatHistory)

                    updateChatControls()
                }
            },

            onError = { error ->

                isGenerating = false

                if (chatHistory.isNotEmpty() &&
                    chatHistory.last().first == "Элис думает..."
                ) {
                    chatHistory.removeAt(chatHistory.lastIndex)
                }

                val errorText =
                    "Ошибка генерации ответа:\n$error"

                chatHistory.add(Pair(errorText, false))

                if (isChatOpen) {

                    chatScreen?.setMessages(chatHistory)

                    updateChatControls()
                }
            }
        )
    }

    // УВЕДОМЛЕНИЯ

    private fun showMessage(message: String) {

        Toast.makeText(
            this,
            message,
            Toast.LENGTH_SHORT
        ).show()
    }
}
