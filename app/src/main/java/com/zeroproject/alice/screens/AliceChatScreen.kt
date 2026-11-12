
package com.zeroproject.alice.screens

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.text.InputType

import com.zeroproject.alice.chat.AliceSelectableText

class AliceChatScreen(
    private val context: Context,
    private val initialMessages: List<Pair<String, Boolean>>,
    private val initialStatus: String,
    private val initialSendEnabled: Boolean,
    private val onBackClick: () -> Unit,
    private val onSendClick: (String) -> Unit,
    private val onVoiceClick: () -> Unit = {}
) {

    private val navy = Color.rgb(11, 20, 38)
    private val panel = Color.rgb(27, 42, 66)
    private val red = Color.rgb(217, 54, 79)
    private val silver = Color.rgb(198, 205, 216)
    private val muted = Color.rgb(145, 158, 180)

    private lateinit var root: LinearLayout
    private lateinit var messagesLayout: LinearLayout
    private lateinit var messagesScroll: ScrollView
    private lateinit var messageInput: EditText

    private var statusView: TextView? = null
    private var sendButton: Button? = null

    private fun dp(value: Int): Int {
        return (value * context.resources.displayMetrics.density).toInt()
    }

    fun create(): View {

        root = LinearLayout(context).apply {
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

        createHeader()
        addSpace(root, 12)

        createMessages()
        addSpace(root, 10)

        createComposer()

        setMessages(initialMessages)
        updateStatus(initialStatus)
        setSendEnabled(initialSendEnabled)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            root.requestApplyInsets()
        }

        return root
    }

    // ШАПКА ЧАТА

    private fun createHeader() {

        val header = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(8), dp(8), dp(8))
            background = rounded(panel, red, 1, 14)
        }

        val backButton = Button(context).apply {
            text = "‹"
            textSize = 32f
            setTextColor(red)
            isAllCaps = false
            backgroundTintList = ColorStateList.valueOf(panel)

            setOnClickListener {
                onBackClick()
            }
        }

        header.addView(
            backButton,
            LinearLayout.LayoutParams(dp(55), dp(55))
        )

        val titleBox = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), 0, 0, 0)
        }

        titleBox.addView(
            makeText("ALICE", 20f, silver, true)
        )

        statusView = makeText(
            initialStatus,
            10f,
            red
        )

        titleBox.addView(statusView!!)

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
    }

    // ОБЛАСТЬ СООБЩЕНИЙ

    private fun createMessages() {

        messagesScroll = ScrollView(context).apply {
            setBackgroundColor(navy)
            isVerticalScrollBarEnabled = false
            isFillViewport = true
        }

        messagesLayout = LinearLayout(context).apply {
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
    }

    // ПОЛЕ ВВОДА

    private fun createComposer() {

        val composer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = rounded(panel, red, 1, 16)
            setPadding(dp(8), dp(6), dp(8), dp(6))
        }

        messageInput = EditText(context).apply {

            hint = "Напиши Элис..."
            setHintTextColor(muted)
            setTextColor(silver)
            textSize = 15f

            maxLines = 4
            minLines = 1

            setPadding(
                dp(10),
                dp(8),
                dp(10),
                dp(8)
            )

            setBackgroundColor(Color.TRANSPARENT)

            // Разрешаем выделение, копирование и вставку текста.

            isFocusable = true
            isFocusableInTouchMode = true
            isLongClickable = true
            setTextIsSelectable(false)

            // Многострочный ввод.

            inputType =
                InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                InputType.TYPE_TEXT_FLAG_CAP_SENTENCES

            imeOptions = EditorInfo.IME_ACTION_SEND

            // Не блокируем стандартное Android-контекстное меню.

            customSelectionActionModeCallback = null
            customInsertionActionModeCallback = null

            setOnEditorActionListener { _, actionId, _ ->

                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    sendCurrentMessage()
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

        sendButton = Button(context).apply {
            text = "➤"
            textSize = 22f
            setTextColor(silver)
            isAllCaps = false
            background = rounded(red, red, 1, 12)

            setOnClickListener {
                sendCurrentMessage()
            }
        }

        val voiceButton = Button(context).apply {
            text = "🎤"
            textSize = 20f
            contentDescription = "Голосовой ввод"
            isAllCaps = false
            backgroundTintList = ColorStateList.valueOf(panel)
            setOnClickListener { onVoiceClick() }
        }

        composer.addView(
            voiceButton,
            LinearLayout.LayoutParams(dp(48), dp(50))
        )

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
    }

    // ОТПРАВКА СООБЩЕНИЯ

    private fun sendCurrentMessage() {

        val message = messageInput.text.toString().trim()

        if (message.isEmpty()) return

        messageInput.setText("")

        onSendClick(message)
    }

    // ДОБАВЛЕНИЕ СООБЩЕНИЯ

    fun addMessage(text: String, isUser: Boolean) {

        val wrapper = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = if (isUser) Gravity.END else Gravity.START
            setPadding(0, dp(5), 0, dp(5))
        }

        val bubble = AliceSelectableText.create(
            context,
            text,
            silver,
            15f
        ).apply {

            includeFontPadding = true

            setPadding(
                dp(15),
                dp(12),
                dp(15),
                dp(12)
            )

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

        scrollToBottom()
    }

    // ОБНОВЛЕНИЕ ИСТОРИИ

    fun setMessages(messages: List<Pair<String, Boolean>>) {

        messagesLayout.removeAllViews()

        for (message in messages) {
            addMessage(message.first, message.second)
        }

        scrollToBottom()
    }

    // СОСТОЯНИЕ ИИ

    fun updateStatus(text: String) {
        statusView?.text = text
    }

    fun setSendEnabled(enabled: Boolean) {

        sendButton?.isEnabled = enabled

        sendButton?.alpha = if (enabled) {
            1f
        } else {
            0.55f
        }
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

        return TextView(context).apply {
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
            TextView(context),
            LinearLayout.LayoutParams(1, dp(height))
        )
    }
}
