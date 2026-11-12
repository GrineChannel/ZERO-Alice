
package com.zeroproject.alice

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.os.Build
import android.view.WindowManager
import android.widget.Toast
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import java.util.Locale

import com.zeroproject.alice.screens.AliceHomeScreen
import com.zeroproject.alice.screens.AliceModelScreen
import com.zeroproject.alice.screens.AliceChatScreen
import com.zeroproject.alice.screens.AliceDeviceScreen
import com.zeroproject.alice.screens.AliceEngineerScreen
import com.zeroproject.alice.engineer.minecraft.AliceMinecraftEngineer
import com.zeroproject.alice.engineer.AliceEngineerFileWriter
import com.zeroproject.alice.device.AliceDeviceActions
import com.zeroproject.alice.tools.AliceToolRegistry
import com.zeroproject.alice.files.AliceWorkspace
import com.zeroproject.alice.files.AliceFileRevision
import com.zeroproject.alice.tasks.AliceTaskStore
import com.zeroproject.alice.tasks.AliceTaskCommands
import com.zeroproject.alice.tasks.AliceTaskExecutor
import com.zeroproject.alice.tasks.AliceTaskLog

import com.zeroproject.alice.memory.AliceMemory
import com.zeroproject.alice.chat.AliceChatManager
import com.zeroproject.alice.navigation.AliceNavigation
import com.zeroproject.alice.ai.AliceAIManager

import java.io.File

class MainActivity : Activity() {

    private val voiceRequestCode = 2301
    private var voiceReplyPending = false
    private var speechOutput: TextToSpeech? = null
    private var speechReady = false

    private fun startVoiceInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Скажи что-нибудь Элис")
        }
        try {
            startActivityForResult(intent, voiceRequestCode)
        } catch (error: Exception) {
            showMessage("Распознавание речи недоступно на устройстве")
        }
    }

    @Deprecated("Legacy activity result API")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != voiceRequestCode || resultCode != RESULT_OK) return
        val spoken = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()?.trim().orEmpty()
        if (spoken.isBlank()) return
        if (!localEngine.isReady()) {
            voiceReplyPending = false
            showChat()
            showMessage("Дождись запуска локального ИИ и повтори голосовой запрос")
            return
        }
        if (aiState.isGenerating) {
            voiceReplyPending = false
            showMessage("Элис ещё отвечает на предыдущий запрос")
            return
        }
        voiceReplyPending = true
        if (!isChatOpen) showChat()
        sendMessage(spoken)
    }

    private fun speakAnswer(answer: String) {
        if (!speechReady) {
            showMessage("Синтез речи пока недоступен")
            return
        }
        speechOutput?.speak(answer, TextToSpeech.QUEUE_FLUSH, null, "alice-reply")
    }


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
    private var deviceScreenOpen = false

    private lateinit var aiManager: AliceAIManager
    private val aiState get() = aiManager.state

    private var lastBackPressedAt = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        speechOutput = TextToSpeech(this) { status ->
            speechReady = status == TextToSpeech.SUCCESS
            if (speechReady) {
                speechOutput?.language = Locale.forLanguageTag("ru-RU")
            }
        }
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
        if (minecraftEngineer.interruptedTasksOnStartup > 0) {
            Toast.makeText(
                this,
                "Прерванных Minecraft-задач: ${minecraftEngineer.interruptedTasksOnStartup}. Проверь журнал разработки.",
                Toast.LENGTH_LONG
            ).show()
        }

        window.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
        )

        showHome()
    }

    override fun onDestroy() {

        speechOutput?.stop()
        speechOutput?.shutdown()
        speechOutput = null

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

        if (deviceScreenOpen || navigation.canNavigateBack) {
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

        deviceScreenOpen = false
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
                startVoiceInput()
            },

            onModelClick = {
                showModelScreen()
            },

            onSettingsClick = {
                showMessage(
                    "Настройки будут добавлены следующим этапом"
                )
            },
            onEngineerClick = { showEngineer() },
            onDeviceClick = { showDeviceControls() }
        )

        setContentView(homeScreen.create())
    }

    private fun showDeviceControls() {
        deviceScreenOpen = true
        chatScreen = null
        modelScreen = null
        engineerScreen = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(true)
        }
        setContentView(AliceDeviceScreen(this, onBack = { showHome() }).create())
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
            },
            onVoiceClick = {
                startVoiceInput()
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
                val text = "Не удалось запустить локальную модель:\n$error"

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

        if (aiState.isGenerating) return

        // Explicit Minecraft engineering commands use the existing shared Gemma engine.
        val command = message.trim()
        val lowerCommand = command.lowercase()
        if (lowerCommand == "покажи прерванные minecraft задачи элис") {
            val failed = minecraftEngineer.taskCore.getTasks().filter {
                it.status == com.zeroproject.alice.engineer.EngineerTaskStatus.FAILED
            }
            val reply = if (failed.isEmpty()) "Прерванных или неудачных Minecraft-задач нет."
                else failed.take(20).joinToString("\\n") {
                    "${it.id.take(8)} | ${it.goal.take(70)} | ${it.message.take(100)}"
                }
            chatHistory.add(Pair(message, true))
            chatHistory.add(Pair(reply, false))
            chatScreen?.setMessages(chatHistory)
            return
        }
        if (lowerCommand.startsWith("покажи копии minecraft элис ")) {
            val prefix = command.substring("покажи копии minecraft элис ".length).trim()
            val matches = if (prefix.length >= 4) minecraftEngineer.projectManager.listProjects().filter {
                it.id.startsWith(prefix, ignoreCase = true)
            } else emptyList()
            val reply = if (matches.size != 1) {
                "Укажи однозначный ID Minecraft-проекта (минимум 4 символа)."
            } else try {
                AliceEngineerFileWriter(minecraftEngineer.projectManager)
                    .listBackups(matches.single().id)
            } catch (error: Exception) {
                "Ошибка просмотра резервных копий: ${error.message}"
            }
            chatHistory.add(Pair(message, true))
            chatHistory.add(Pair(reply, false))
            chatScreen?.setMessages(chatHistory)
            return
        }
        if (lowerCommand.startsWith("примени версию minecraft элис ")) {
            val args = command.substring("примени версию minecraft элис ".length).trim().split(Regex("\\s+"))
            val prefix = args.getOrNull(0).orEmpty()
            val batchPrefix = args.getOrNull(1).orEmpty()
            val matches = if (prefix.length >= 4) minecraftEngineer.projectManager.listProjects().filter {
                it.id.startsWith(prefix, ignoreCase = true)
            } else emptyList()
            val reply = if (matches.size != 1 || batchPrefix.length < 4 || args.size != 2) {
                "Формат: примени версию minecraft элис ID_проекта ID_версии"
            } else if (engineerBusy) {
                "Дождись завершения текущей разработки."
            } else try {
                val result = AliceEngineerFileWriter(minecraftEngineer.projectManager)
                    .applyPreview(matches.single().id, batchPrefix)
                "Версия применена: файлов ${result.writtenFiles.size}, новых ${result.createdFiles.size}, резервных копий ${result.backupFiles.size}. Предварительная версия сохранена."
            } catch (error: Exception) {
                "Не удалось применить версию: ${error.message}"
            }
            chatHistory.add(Pair(message, true))
            chatHistory.add(Pair(reply, false))
            chatScreen?.setMessages(chatHistory)
            return
        }
        if (lowerCommand.startsWith("проверь версию minecraft элис ")) {
            val args = command.substring("проверь версию minecraft элис ".length).trim().split(Regex("\\s+"))
            val prefix = args.getOrNull(0).orEmpty()
            val batchPrefix = args.getOrNull(1).orEmpty()
            val matches = if (prefix.length >= 4) minecraftEngineer.projectManager.listProjects().filter {
                it.id.startsWith(prefix, ignoreCase = true)
            } else emptyList()
            val reply = if (matches.size != 1 || batchPrefix.length < 4 || args.size != 2) {
                "Формат: проверь версию minecraft элис ID_проекта ID_версии"
            } else try {
                minecraftEngineer.projectManager.inspectPreviewBatch(matches.single().id, batchPrefix)
            } catch (error: Exception) {
                "Ошибка проверки версии: ${error.message}"
            }
            chatHistory.add(Pair(message, true))
            chatHistory.add(Pair(reply, false))
            chatScreen?.setMessages(chatHistory)
            return
        }
        if (lowerCommand.startsWith("покажи версии minecraft элис ")) {
            val prefix = command.substring("покажи версии minecraft элис ".length).trim()
            val matches = if (prefix.length >= 4) minecraftEngineer.projectManager.listProjects().filter {
                it.id.startsWith(prefix, ignoreCase = true)
            } else emptyList()
            val reply = if (matches.size != 1) {
                "Укажи однозначный ID Minecraft-проекта (минимум 4 символа)."
            } else try {
                minecraftEngineer.projectManager.listPreviewBatches(matches.single().id)
            } catch (error: Exception) {
                "Ошибка просмотра предварительных версий: ${error.message}"
            }
            chatHistory.add(Pair(message, true))
            chatHistory.add(Pair(reply, false))
            chatScreen?.setMessages(chatHistory)
            return
        }
        if (lowerCommand.startsWith("проверь файлы minecraft элис ")) {
            val prefix = command.substring("проверь файлы minecraft элис ".length).trim()
            val matches = if (prefix.length >= 4) minecraftEngineer.projectManager.listProjects().filter {
                it.id.startsWith(prefix, ignoreCase = true)
            } else emptyList()
            val reply = if (matches.size != 1) {
                "Укажи однозначный ID Minecraft-проекта (минимум 4 символа)."
            } else try {
                minecraftEngineer.projectManager.inspectProjectFiles(matches.single().id)
            } catch (error: Exception) {
                "Ошибка проверки файлов: ${error.message}"
            }
            chatHistory.add(Pair(message, true))
            chatHistory.add(Pair(reply, false))
            chatScreen?.setMessages(chatHistory)
            return
        }
        if (lowerCommand.startsWith("подготовь повтор minecraft элис ")) {
            val prefix = command.substring("подготовь повтор minecraft элис ".length).trim()
            val matches = if (prefix.length >= 4) minecraftEngineer.taskCore.getTasks().filter {
                it.id.startsWith(prefix, ignoreCase = true)
            } else emptyList()
            val reply = if (matches.size != 1) {
                "Укажи однозначный ID задачи (минимум 4 символа)."
            } else try {
                val retry = minecraftEngineer.taskCore.prepareRetry(matches.single().id)
                "Создана новая ожидающая задача ${retry.id.take(8)} для проекта ${retry.projectId.take(8)}. Файлы не изменены. Перед запуском проверь существующие файлы в разделе разработки."
            } catch (error: Exception) {
                "Не удалось подготовить повтор: ${error.message}"
            }
            chatHistory.add(Pair(message, true))
            chatHistory.add(Pair(reply, false))
            chatScreen?.setMessages(chatHistory)
            return
        }
        if (lowerCommand == "покажи minecraft проекты элис") {
            val projects = minecraftEngineer.projectManager.listProjects()
            val reply = if (projects.isEmpty()) "Minecraft-проектов пока нет."
                else projects.take(20).joinToString("\\n") {
                    "${it.id.take(8)} | ${it.name}"
                }
            chatHistory.add(Pair(message, true))
            chatHistory.add(Pair(reply, false))
            chatScreen?.setMessages(chatHistory)
            return
        }
        if (lowerCommand.startsWith("разработай minecraft элис ") ||
            lowerCommand.startsWith("предпросмотр minecraft элис ")) {
            val previewOnly = lowerCommand.startsWith("предпросмотр minecraft элис ")
            val commandPrefix = if (previewOnly) "предпросмотр minecraft элис " else "разработай minecraft элис "
            val args = command.substring(commandPrefix.length)
            val separator = args.indexOf(" :: ")
            val prefix = if (separator > 0) args.substring(0, separator).trim() else ""
            val goal = if (separator > 0) args.substring(separator + 4).trim() else ""
            val projects = if (prefix.length >= 4)
                minecraftEngineer.projectManager.listProjects().filter {
                    it.id.startsWith(prefix, ignoreCase = true)
                } else emptyList()
            val failure = when {
                separator <= 0 || goal.isBlank() -> "Формат: разработай minecraft элис ID :: описание задачи"
                projects.size != 1 -> "Укажи однозначный ID Minecraft-проекта (минимум 4 символа)."
                engineerBusy -> "Разработка уже выполняется."
                !localEngine.isReady() -> "Локальная модель ещё не готова."
                else -> null
            }
            chatHistory.add(Pair(message, true))
            if (failure != null) {
                chatHistory.add(Pair(failure, false))
                chatScreen?.setMessages(chatHistory)
                return
            }
            val project = projects.single()
            engineerBusy = true
            chatHistory.add(Pair(if (previewOnly) "Готовлю предварительные файлы: ${project.name}." else "Запускаю разработку: ${project.name}.", false))
            chatScreen?.setMessages(chatHistory)
            minecraftEngineer.develop(
                projectId = project.id,
                goal = goal,
                previewOnly = previewOnly,
                onProgress = { progress ->
                    runOnUiThread {
                        if (isChatOpen) {
                            chatScreen?.updateStatus(progress)
                        }
                    }
                },
                onResult = { result ->
                    runOnUiThread {
                        engineerBusy = false
                        chatHistory.add(Pair(if (previewOnly) "Предварительные файлы сохранены: ${result.project.name}. Оригиналы не изменены." else "Разработка завершена: ${result.project.name}. Проверка: ${result.testReport.status}.", false))
                        if (isChatOpen) chatScreen?.setMessages(chatHistory)
                    }
                },
                onError = { error ->
                    runOnUiThread {
                        engineerBusy = false
                        chatHistory.add(Pair("Ошибка Minecraft-разработки: $error", false))
                        if (isChatOpen) chatScreen?.setMessages(chatHistory)
                    }
                }
            )
            return
        }

        // Device actions require an exact user command. They never run
        // because of text generated by the AI model.
        val workspace = AliceWorkspace(this)
        val taskStore = AliceTaskStore(this)
        val taskLog = AliceTaskLog(this)
        val revisions = AliceFileRevision(this, workspace)
        val toolResult = AliceToolRegistry(
            AliceDeviceActions(this),
            workspace,
            AliceTaskCommands(
                taskStore,
                AliceTaskExecutor(
                    taskStore, workspace, taskLog, revisions, minecraftEngineer.projectManager
                ) { projectPrefix, goal, completed ->
                    val matches = minecraftEngineer.projectManager.listProjects().filter {
                        it.id.startsWith(projectPrefix, ignoreCase = true)
                    }
                    if (projectPrefix.length < 4 || matches.size != 1 ||
                        engineerBusy || !localEngine.isReady()) {
                        false
                    } else {
                        engineerBusy = true
                        try {
                            minecraftEngineer.develop(
                                projectId = matches.single().id,
                                goal = goal,
                                onProgress = { progress ->
                                    runOnUiThread {
                                        if (isChatOpen) chatScreen?.updateStatus(progress)
                                    }
                                },
                                onResult = { result ->
                                    runOnUiThread {
                                        engineerBusy = false
                                        completed(true, "Minecraft: ${result.project.name}; проверка ${result.testReport.status}")
                                        chatHistory.add(Pair("Minecraft-задача завершена: ${result.project.name}", false))
                                        if (isChatOpen) chatScreen?.setMessages(chatHistory)
                                    }
                                },
                                onError = { error ->
                                    runOnUiThread {
                                        engineerBusy = false
                                        completed(false, "Minecraft-разработка: $error")
                                        chatHistory.add(Pair("Ошибка Minecraft-задачи: $error", false))
                                        if (isChatOpen) chatScreen?.setMessages(chatHistory)
                                    }
                                }
                            )
                            true
                        } catch (error: Exception) {
                            engineerBusy = false
                            false
                        }
                    }
                },
                taskLog
            ),
            revisions
        ).executeUserCommand(message)
        if (toolResult.handled) {
            chatHistory.add(Pair(message, true))
            chatScreen?.addMessage(message, true)
            chatHistory.add(Pair(toolResult.message, false))
            chatScreen?.addMessage(toolResult.message, false)
            return
        }

        if (!localEngine.isReady()) {
            showMessage("Элис ещё загружает локальную модель")
            return
        }

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

                if (voiceReplyPending) {
                    voiceReplyPending = false
                    speakAnswer(answer)
                }

                if (isChatOpen) {

                    chatScreen?.setMessages(chatHistory)

                    updateChatControls()
                }
            },

            onHistoryChanged = {
                // Rebuild the visible transcript from SQLite after a forget
                // command. This also removes messages deleted from storage.
                chatHistory.clear()
                chatManager.getHistory().forEach { saved ->
                    chatHistory.add(Pair(saved.content, saved.role == "user"))
                }
                if (isChatOpen) {
                    chatScreen?.setMessages(chatHistory)
                }
            },
            onError = { error ->

                voiceReplyPending = false
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
