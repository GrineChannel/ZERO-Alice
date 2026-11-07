
package com.zeroproject.alice

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import android.view.WindowInsets
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import android.widget.ProgressBar

import java.io.File

class MainActivity : Activity() {

    private val navy = Color.rgb(11, 20, 38)
    private val panel = Color.rgb(27, 42, 66)
    private val red = Color.rgb(217, 54, 79)
    private val silver = Color.rgb(198, 205, 216)
    private val muted = Color.rgb(145, 158, 180)

    private lateinit var messagesLayout: LinearLayout
    private lateinit var messagesScroll: ScrollView
    private lateinit var messageInput: EditText

    private lateinit var modelDownloader: AliceModelDownloader
    private lateinit var localEngine: AliceLocalEngine

    private var modelProgress: ProgressBar? = null
    private var modelStatus: TextView? = null
    private var modelSize: TextView? = null
    private var modelActionButton: Button? = null

    private var chatStatus: TextView? = null
    private var sendButton: Button? = null

    private val chatHistory = mutableListOf<Pair<String, Boolean>>()

    private var isChatOpen = false
    private var isModelScreenOpen = false
    private var isDownloading = false

    private var isInitializingAI = false
    private var isGenerating = false
    private var engineInitializationStarted = false

    private var lastBackPressedAt = 0L

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        modelDownloader = AliceModelDownloader(this)
        localEngine = AliceLocalEngine(this)

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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(true)
        }

        val scroll = ScrollView(this).apply {
            setFillViewport(true)
            setBackgroundColor(navy)
            isVerticalScrollBarEnabled = false
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(24), dp(32), dp(24), dp(32))
            setBackgroundColor(navy)
        }

        scroll.addView(
            root,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        val logoPanel = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            orientation = LinearLayout.VERTICAL
            background = rounded(panel, red, 1, 16)
            setPadding(dp(12), dp(12), dp(12), dp(12))
        }

        logoPanel.addView(makeText("ZERO", 42f, silver, true))
        logoPanel.addView(makeText("ZERO PROJECT", 12f, muted))

        root.addView(
            logoPanel,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(105)
            )
        )

        addSpace(root, 35)

        val avatar = TextView(this).apply {
            text = "A"
            textSize = 90f
            includeFontPadding = true
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(red)
            background = rounded(panel, red, 2, 28)
        }

        root.addView(
            avatar,
            LinearLayout.LayoutParams(dp(200), dp(200))
        )

        addSpace(root, 24)

        root.addView(makeText("ALICE", 34f, silver, true))
        root.addView(makeText("ТВОЙ ЦИФРОВОЙ НАПАРНИК", 12f, muted))

        addSpace(root, 16)

        root.addView(
            makeText("●  СИСТЕМА АКТИВНА", 14f, red, true)
        )

        addSpace(root, 35)

        root.addView(makeButton("ОБЩЕНИЕ") {
            showChat()
        })

        addSpace(root, 14)

        root.addView(makeButton("ГОЛОС") {
            showMessage("Голосовой модуль будет добавлен позже")
        })

        addSpace(root, 14)

        root.addView(makeButton("МОДЕЛЬ ИИ") {
            showModelScreen()
        })

        addSpace(root, 14)

        root.addView(makeButton("НАСТРОЙКИ") {
            showMessage("Настройки будут добавлены следующим этапом")
        })

        addSpace(root, 35)

        root.addView(makeText("ZERO PROJECT • 2026", 11f, muted))

        setContentView(scroll)
    }

    // ЭКРАН МОДЕЛИ ИИ

    private fun showModelScreen() {

        isChatOpen = false
        isModelScreenOpen = true

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(true)
        }

        val modelFile = File(filesDir, "gemma-4-E2B-it.litertlm")
        val modelExists = modelDownloader.isModelDownloaded()

        val scroll = ScrollView(this).apply {
            setFillViewport(true)
            setBackgroundColor(navy)
            isVerticalScrollBarEnabled = false
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(22), dp(24), dp(22), dp(28))
            setBackgroundColor(navy)
        }

        scroll.addView(
            root,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        root.addView(makeText("ЛОКАЛЬНЫЙ ИИ", 25f, silver, true))

        addSpace(root, 8)

        root.addView(makeText("ZERO: ALICE", 13f, red, true))

        addSpace(root, 28)

        val modelPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = rounded(panel, red, 1, 18)
            setPadding(dp(18), dp(22), dp(18), dp(22))
        }

        modelPanel.addView(
            makeText("GEMMA 4", 30f, silver, true)
        )

        addSpace(modelPanel, 6)

        modelPanel.addView(
            makeText("E2B INSTRUCT", 17f, red, true)
        )

        addSpace(modelPanel, 18)

        modelPanel.addView(
            makeText("Локальная языковая модель", 14f, silver)
        )

        modelPanel.addView(
            makeText("Работа непосредственно на телефоне", 12f, muted)
        )

        modelPanel.addView(
            makeText("Размер файла: около 2,59 ГБ", 13f, silver)
        )

        root.addView(
            modelPanel,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        addSpace(root, 30)

        root.addView(
            makeText("СОСТОЯНИЕ МОДЕЛИ", 14f, muted, true)
        )

        addSpace(root, 10)

        modelStatus = makeText(
            when {
                isDownloading -> "Загрузка модели продолжается..."
                modelExists -> "Модель уже загружена!"
                else -> "Модель ещё не загружена"
            },
            14f,
            silver
        )

        root.addView(modelStatus!!)

        addSpace(root, 18)

        modelProgress = ProgressBar(
            this,
            null,
            android.R.attr.progressBarStyleHorizontal
        ).apply {
            max = 100
            progress = if (modelExists) 100 else 0
            progressTintList =
                android.content.res.ColorStateList.valueOf(red)
            progressBackgroundTintList =
                android.content.res.ColorStateList.valueOf(panel)
        }

        root.addView(
            modelProgress,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(12)
            )
        )

        addSpace(root, 10)

        modelSize = makeText(
            if (modelExists) {
                formatBytes(modelFile.length())
            } else {
                "0 МБ / 0 МБ"
            },
            12f,
            muted
        )

        root.addView(modelSize!!)

        addSpace(root, 25)

        modelActionButton = makeButton(
            when {
                isDownloading -> "ОТМЕНИТЬ ЗАГРУЗКУ"
                modelExists -> "МОДЕЛЬ ЗАГРУЖЕНА"
                else -> "СКАЧАТЬ МОДЕЛЬ"
            }
        ) {
            if (isDownloading) {
                cancelModelDownload()
            } else if (!modelDownloader.isModelDownloaded()) {
                startModelDownload()
            }
        }.apply {
            isEnabled = !modelExists
            alpha = if (modelExists) 0.65f else 1f
        }

        root.addView(modelActionButton!!)

        addSpace(root, 16)

        root.addView(
            makeText(
                "После загрузки файл будет сохранён во внутренней памяти приложения.",
                12f,
                muted
            )
        )

        addSpace(root, 25)

        root.addView(makeButton("НАЗАД") {
            showHome()
        })

        setContentView(scroll)
    }

    // ЗАПУСК ЗАГРУЗКИ

    private fun startModelDownload() {

        if (isDownloading) return

        isDownloading = true

        modelActionButton?.text = "ОТМЕНИТЬ ЗАГРУЗКУ"
        modelActionButton?.isEnabled = true
        modelStatus?.text = "Подключение к серверу..."

        modelDownloader.start(
            object : AliceModelDownloader.Listener {

                override fun onProgress(
                    downloaded: Long,
                    total: Long,
                    percent: Int
                ) {
                    if (!isModelScreenOpen) return

                    modelProgress?.progress = percent

                    modelStatus?.text =
                        "Загрузка модели: $percent%"

                    modelSize?.text =
                        "${formatBytes(downloaded)} / ${formatBytes(total)}"
                }

                override fun onComplete(file: File) {

                    isDownloading = false

                    modelProgress?.progress = 100

                    modelStatus?.text =
                        "Модель успешно загружена и проверена!"

                    modelSize?.text =
                        formatBytes(file.length())

                    modelActionButton?.text = "МОДЕЛЬ ЗАГРУЖЕНА"
                    modelActionButton?.isEnabled = false
                    modelActionButton?.alpha = 0.65f

                    showMessage(
                        "Gemma 4 E2B успешно загружена!"
                    )
                }

                override fun onError(message: String) {

                    isDownloading = false

                    modelStatus?.text =
                        "Ошибка загрузки: $message"

                    modelActionButton?.text =
                        "ПОВТОРИТЬ ЗАГРУЗКУ"

                    modelActionButton?.isEnabled = true
                    modelActionButton?.alpha = 1f

                    showMessage(message)
                }

                override fun onCancelled() {

                    isDownloading = false

                    modelStatus?.text =
                        "Загрузка отменена. Можно продолжить позже."

                    modelActionButton?.text =
                        "ПРОДОЛЖИТЬ ЗАГРУЗКУ"

                    modelActionButton?.isEnabled = true
                    modelActionButton?.alpha = 1f
                }
            }
        )
    }

    // ОТМЕНА ЗАГРУЗКИ

    private fun cancelModelDownload() {

        if (!isDownloading) return

        modelStatus?.text = "Отмена загрузки..."

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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

            window.setDecorFitsSystemWindows(false)
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(navy)
            setPadding(dp(16), dp(12), dp(16), dp(12))
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

            root.setOnApplyWindowInsetsListener { view, insets ->

                val systemBars = insets.getInsets(
                    WindowInsets.Type.systemBars()
                )

                val keyboard = insets.getInsets(
                    WindowInsets.Type.ime()
                )

                view.setPadding(
                    dp(16) + systemBars.left,
                    dp(12) + systemBars.top,
                    dp(16) + systemBars.right,
                    dp(12) + maxOf(
                        systemBars.bottom,
                        keyboard.bottom
                    )
                )

                insets
            }
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(8), dp(8), dp(8))
            background = rounded(panel, red, 1, 14)
        }

        val backButton = Button(this).apply {
            text = "‹"
            textSize = 32f
            setTextColor(red)
            isAllCaps = false
            backgroundTintList =
                android.content.res.ColorStateList.valueOf(panel)

            setOnClickListener {
                showHome()
            }
        }

        header.addView(
            backButton,
            LinearLayout.LayoutParams(dp(55), dp(55))
        )

        val titleBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), 0, 0, 0)
        }

        titleBox.addView(makeText("ALICE", 20f, silver, true))

        chatStatus = makeText(
            getAIStatusText(),
            10f,
            red
        )

        titleBox.addView(chatStatus!!)

        header.addView(
            titleBox,
            LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        root.addView(
            header,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(72)
            )
        )

        addSpace(root, 12)

        messagesScroll = ScrollView(this).apply {
            setBackgroundColor(navy)
            isVerticalScrollBarEnabled = false
            isFillViewport = true
        }

        messagesLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(4), dp(12), dp(4), dp(12))
        }

        messagesScroll.addView(
            messagesLayout,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        root.addView(
            messagesScroll,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        addSpace(root, 10)

        val composer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = rounded(panel, red, 1, 16)
            setPadding(dp(8), dp(6), dp(8), dp(6))
        }

        messageInput = EditText(this).apply {
            hint = "Напиши Элис..."
            setHintTextColor(muted)
            setTextColor(silver)
            textSize = 15f
            maxLines = 4
            minLines = 1
            setPadding(dp(10), dp(8), dp(10), dp(8))
            setBackgroundColor(Color.TRANSPARENT)

            imeOptions = EditorInfo.IME_ACTION_SEND

            inputType =
                android.text.InputType.TYPE_CLASS_TEXT or
                    android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE

            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    sendMessage()
                    true
                } else {
                    false
                }
            }
        }

        composer.addView(
            messageInput,
            LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        sendButton = Button(this).apply {
            text = "➤"
            textSize = 22f
            setTextColor(silver)
            isAllCaps = false
            background = rounded(red, red, 1, 12)

            setOnClickListener {
                sendMessage()
            }
        }

        composer.addView(
            sendButton!!,
            LinearLayout.LayoutParams(dp(55), dp(50))
        )

        root.addView(
            composer,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        setContentView(root)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            root.requestApplyInsets()
        }

        if (chatHistory.isEmpty()) {
            chatHistory.add(
                Pair(
                    "Привет! Я Элис. ❤️\n\nЯ запускаю свой локальный ИИ. Немного терпения!",
                    false
                )
            )
        }

        messagesLayout.removeAllViews()

        for (message in chatHistory) {
            addBubble(message.first, message.second)
        }

        scrollToBottom()
        updateChatControls()

        // Запускаем Gemma при первом открытии чата.
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

        chatStatus?.text = getAIStatusText()

        sendButton?.isEnabled =
            localEngine.isReady() && !isGenerating

        sendButton?.alpha =
            if (sendButton?.isEnabled == true) 1f else 0.55f
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

            chatHistory.add(
                Pair(
                    "Я пока не могу запуститься: модель Gemma не найдена. Открой раздел «Модель ИИ» и проверь загрузку.",
                    false
                )
            )

            if (isChatOpen) {
                addBubble(chatHistory.last().first, false)
            }

            updateChatControls()
            return
        }

        isInitializingAI = true
        engineInitializationStarted = true

        chatStatus?.text = "●  ЗАГРУЗКА GEMMA..."
        updateChatControls()

        localEngine.initialize(

            onReady = {

                isInitializingAI = false

                if (isChatOpen) {

                    chatStatus?.text =
                        "●  ЛОКАЛЬНЫЙ ИИ АКТИВЕН"

                    val text =
                        "Я проснулась! ❤️ Теперь я могу отвечать с помощью локальной Gemma."

                    chatHistory.add(Pair(text, false))
                    addBubble(text, false)

                    updateChatControls()
                    scrollToBottom()
                }
            },

            onError = { error ->

                isInitializingAI = false

                if (isChatOpen) {

                    val text =
                        "Не удалось запустить локальную модель:\n$error"

                    chatHistory.add(Pair(text, false))
                    addBubble(text, false)

                    chatStatus?.text = "●  ОШИБКА ЗАПУСКА ИИ"

                    updateChatControls()
                    scrollToBottom()
                }

                showMessage("Ошибка запуска Gemma: $error")
            }
        )
    }

    // ОТПРАВКА СООБЩЕНИЯ В GEMMA

    private fun sendMessage() {

        val message = messageInput.text.toString().trim()

        if (message.isEmpty()) return

        if (!localEngine.isReady()) {

            showMessage(
                "Элис ещё загружает локальную модель"
            )

            return
        }

        if (isGenerating) return

        messageInput.setText("")

        chatHistory.add(Pair(message, true))
        addBubble(message, true)

        isGenerating = true

        chatHistory.add(
            Pair("Элис думает...", false)
        )

        addBubble("Элис думает...", false)

        updateChatControls()
        scrollToBottom()

        localEngine.sendMessage(

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

                    messagesLayout.removeAllViews()

                    for (item in chatHistory) {
                        addBubble(item.first, item.second)
                    }

                    updateChatControls()
                    scrollToBottom()
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

                    messagesLayout.removeAllViews()

                    for (item in chatHistory) {
                        addBubble(item.first, item.second)
                    }

                    updateChatControls()
                    scrollToBottom()
                }
            }
        )
    }

    // ОТОБРАЖЕНИЕ СООБЩЕНИЙ

    private fun addBubble(text: String, isUser: Boolean) {

        val wrapper = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = if (isUser) Gravity.END else Gravity.START
            setPadding(0, dp(5), 0, dp(5))
        }

        val bubble = TextView(this).apply {
            this.text = text
            textSize = 15f
            setTextColor(silver)
            includeFontPadding = true
            setPadding(dp(15), dp(12), dp(15), dp(12))
            maxWidth = dp(285)

            background = if (isUser) {
                rounded(red, red, 1, 18)
            } else {
                rounded(panel, red, 1, 18)
            }
        }

        wrapper.addView(
            bubble,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        messagesLayout.addView(
            wrapper,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
    }

    private fun scrollToBottom() {
        messagesScroll.post {
            messagesScroll.fullScroll(ScrollView.FOCUS_DOWN)
        }
    }

    // ВСПОМОГАТЕЛЬНЫЕ ЭЛЕМЕНТЫ

    private fun makeText(
        value: String,
        size: Float,
        color: Int,
        bold: Boolean = false
    ): TextView {

        return TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            gravity = Gravity.CENTER
            includeFontPadding = true

            if (bold) {
                typeface = Typeface.DEFAULT_BOLD
            }
        }
    }

    private fun makeButton(
        label: String,
        action: () -> Unit
    ): Button {

        return Button(this).apply {
            text = label
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(silver)
            gravity = Gravity.CENTER
            isAllCaps = false
            minHeight = dp(68)
            minimumHeight = dp(68)
            stateListAnimator = null

            backgroundTintList =
                android.content.res.ColorStateList.valueOf(panel)

            setPadding(dp(16), dp(12), dp(16), dp(12))

            setOnClickListener {
                action()
            }

            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(68)
            )
        }
    }

    private fun rounded(
        color: Int,
        strokeColor: Int,
        strokeWidth: Int,
        radius: Int
    ): GradientDrawable {

        return GradientDrawable().apply {
            setColor(color)
            setCornerRadius(dp(radius).toFloat())
            setStroke(dp(strokeWidth), strokeColor)
        }
    }

    private fun addSpace(
        layout: LinearLayout,
        height: Int
    ) {

        layout.addView(
            TextView(this),
            LinearLayout.LayoutParams(1, dp(height))
        )
    }

    private fun showMessage(message: String) {
        Toast.makeText(
            this,
            message,
            Toast.LENGTH_SHORT
        ).show()
    }
}
