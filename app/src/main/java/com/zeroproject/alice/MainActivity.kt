
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
import com.zeroproject.alice.screens.AliceEngineerScreen
import com.zeroproject.alice.engineer.minecraft.AliceMinecraftEngineer

import com.zeroproject.alice.memory.AliceMemory
import com.zeroproject.alice.chat.AliceChatManager
import com.zeroproject.alice.navigation.AliceNavigation
import com.zeroproject.alice.ai.AliceAIManager

import java.io.File

class MainActivity : Activity() {

    private lateinit var modelDownloader: AliceModelDownloader
    private lateinit var localEngine: AliceLocalEngine

    private lateinit var aliceMemory: AliceMemory
    private lateinit var chatManager: AliceChatManager
    private lateinit var minecraftEngineer: AliceMinecraftEngineer
    private var engineerScreen: AliceEngineerScreen? = null
    private var engineerBusy = false

    private var modelScreen: AliceModelScreen? = null
    private var chatScreen: AliceChatScreen? = null

    private val chatHistory = mutableListOf<Pair<String, Boolean>>()

    private val navigation = AliceNavigation()
    private val isChatOpen get() = navigation.isChatOpen
    private val isModelScreenOpen get() = navigation.isModelScreenOpen
    private val isEngineerOpen get() = navigation.isEngineerOpen
    private var isDownloading = false

    private lateinit var aiManager: AliceAIManager
    private val aiState get() = aiManager.state

    private var lastBackPressedAt = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        modelDownloader = AliceModelDownloader(this)
        localEngine = AliceLocalEngine(this)
        aiManager = AliceAIManager(
            File(filesDir, "gemma-4-E2B-it.litertlm"),
            modelDownloader,
            localEngine
        )

        aliceMemory = AliceMemory(this)
        chatManager = AliceChatManager(
            localEngine,
            aliceMemory
        )
        minecraftEngineer = AliceMinecraftEngineer(this, localEngine)

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

        if (navigation.canNavigateBack) {
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

        navigation.navigateHome()

        chatScreen = null
        engineerScreen = null
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
            },
            onEngineerClick = { showEngineer() }
        )

        setContentView(homeScreen.create())
    }

    // ЭКРАН МОДЕЛИ ИИ

    private fun showModelScreen() {

        navigation.navigateTo(AliceNavigation.Screen.MODEL)
        engineerScreen = null
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

        navigation.navigateTo(AliceNavigation.Screen.CHAT)
        engineerScreen = null
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
                localEngine.isReady() && !aiState.isGenerating,

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

    private fun showEngineer() {
        navigation.navigateTo(AliceNavigation.Screen.ENGINEER)
        chatScreen = null
        modelScreen = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(true)
        }
        val screen = AliceEngineerScreen(
            context = this,
            onBackClick = { showHome() },
            onCreateProject = { name, description ->
                try {
                    val project = minecraftEngineer.createProject(name, description)
                    engineerScreen?.updateProjects(minecraftEngineer.projectManager.listProjects())
                    engineerScreen?.setStatus("Проект создан: ${project.name}. Выбери его в списке.")
                } catch (error: Exception) {
                    engineerScreen?.setStatus("Ошибка создания: ${error.message}")
                }
            },
            onDevelop = { projectId, goal ->
                if (engineerBusy) {
                    engineerScreen?.setStatus("Разработка уже выполняется")
                } else if (!localEngine.isReady()) {
                    engineerScreen?.setStatus("ИИ ещё не готов. Дождись загрузки модели.")
                    initializeAI()
                } else {
                    engineerBusy = true
                    engineerScreen?.setBusy(true)
                    minecraftEngineer.develop(
                        projectId = projectId,
                        goal = goal,
                        onProgress = { message ->
                            runOnUiThread { if (isEngineerOpen) engineerScreen?.setStatus(message) }
                        },
                        onResult = { result ->
                            runOnUiThread {
                                engineerBusy = false
                                if (isEngineerOpen) engineerScreen?.showResult("Готово: ${result.project.name}. Проверка: ${result.testReport.status}")
                            }
                        },
                        onError = { error ->
                            runOnUiThread {
                                engineerBusy = false
                                if (isEngineerOpen) engineerScreen?.showResult("Ошибка разработки: $error")
                            }
                        }
                    )
                }
            }
        )
        engineerScreen = screen
        setContentView(screen.createView())
        screen.updateProjects(minecraftEngineer.projectManager.listProjects())
        screen.setBusy(engineerBusy)
        if (!localEngine.isReady()) {
            screen.setStatus("Подготовка локального ИИ...")
            initializeAI()
        }
    }

    // СОСТОЯНИЕ ИИ

    private fun getAIStatusText(): String =
        aiState.statusText(localEngine.isReady())

    private fun updateChatControls() {

        if (!isChatOpen) return

        chatScreen?.updateStatus(
            getAIStatusText()
        )

        chatScreen?.setSendEnabled(
            localEngine.isReady() && !aiState.isGenerating
        )
    }

    // ИНИЦИАЛИЗАЦИЯ GEMMA

    private fun initializeAI() {

        val result = aiManager.start(
            onReady = {
                if (isEngineerOpen) engineerScreen?.setStatus("Локальный ИИ готов к разработке")

                val text =
                    "Я проснулась! ❤️ Теперь я могу отвечать с помощью локальной Gemma."

                chatHistory.add(Pair(text, false))

                if (isChatOpen) {
                    chatScreen?.addMessage(text, false)
                    updateChatControls()
                }
            },
            onError = { error ->
                val text = "Не удалось запустить локальную модель:\\n$error"

                chatHistory.add(Pair(text, false))

                if (isChatOpen) {
                    chatScreen?.addMessage(text, false)
                    chatScreen?.updateStatus("●  ОШИБКА ЗАПУСКА ИИ")
                    updateChatControls()
                }

                if (isEngineerOpen) engineerScreen?.setStatus(text)
                showMessage("Ошибка запуска Gemma: $error")
            }
        )

        when (result) {
            AliceAIManager.StartResult.READY -> {
                updateChatControls()
                if (isEngineerOpen) engineerScreen?.setStatus("Локальный ИИ готов к разработке")
            }
            AliceAIManager.StartResult.ALREADY_STARTING -> updateChatControls()
            AliceAIManager.StartResult.MODEL_MISSING -> {
                val text =
                    "Я пока не могу запуститься: модель Gemma не найдена. Открой раздел «Модель ИИ» и проверь загрузку."

                chatHistory.add(Pair(text, false))
                chatScreen?.addMessage(text, false)
                updateChatControls()
                if (isEngineerOpen) engineerScreen?.setStatus(text)
            }
            AliceAIManager.StartResult.STARTED -> updateChatControls()
        }
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

        if (aiState.isGenerating) return

        // Показываем сообщение пользователя

        chatHistory.add(Pair(message, true))
        chatScreen?.addMessage(message, true)

        // Показываем индикатор генерации

        aiState.beginGeneration()

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

                aiState.finishGeneration()

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

                aiState.finishGeneration()

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
