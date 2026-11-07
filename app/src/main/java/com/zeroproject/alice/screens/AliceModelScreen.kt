
package com.zeroproject.alice.screens

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView

class AliceModelScreen(
    private val context: Context,
    private val isModelDownloaded: Boolean,
    private val isDownloading: Boolean,
    private val modelSize: String,
    private val onDownloadClick: () -> Unit,
    private val onCancelClick: () -> Unit,
    private val onBackClick: () -> Unit
) {

    private val navy = Color.rgb(11, 20, 38)
    private val panel = Color.rgb(27, 42, 66)
    private val red = Color.rgb(217, 54, 79)
    private val silver = Color.rgb(198, 205, 216)
    private val muted = Color.rgb(145, 158, 180)

    private var statusView: TextView? = null
    private var sizeView: TextView? = null
    private var progressBar: ProgressBar? = null
    private var actionButton: Button? = null

    private fun dp(value: Int): Int {
        return (value * context.resources.displayMetrics.density).toInt()
    }

    fun create(): View {

        val scroll = ScrollView(context).apply {
            setFillViewport(true)
            setBackgroundColor(navy)
            isVerticalScrollBarEnabled = false
        }

        val root = LinearLayout(context).apply {
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

        val modelPanel = LinearLayout(context).apply {
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

        statusView = makeText(
            when {
                isDownloading -> "Загрузка модели продолжается..."
                isModelDownloaded -> "Модель уже загружена!"
                else -> "Модель ещё не загружена"
            },
            14f,
            silver
        )

        root.addView(statusView!!)

        addSpace(root, 18)

        progressBar = ProgressBar(
            context,
            null,
            android.R.attr.progressBarStyleHorizontal
        ).apply {
            max = 100
            progress = if (isModelDownloaded) 100 else 0

            progressTintList = ColorStateList.valueOf(red)
            progressBackgroundTintList = ColorStateList.valueOf(panel)
        }

        root.addView(
            progressBar,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(12)
            )
        )

        addSpace(root, 10)

        sizeView = makeText(
            if (isModelDownloaded) modelSize else "0 МБ / 0 МБ",
            12f,
            muted
        )

        root.addView(sizeView!!)

        addSpace(root, 25)

        actionButton = makeButton(
            when {
                isDownloading -> "ОТМЕНИТЬ ЗАГРУЗКУ"
                isModelDownloaded -> "МОДЕЛЬ ЗАГРУЖЕНА"
                else -> "СКАЧАТЬ МОДЕЛЬ"
            }
        ) {
            if (isDownloading) {
                onCancelClick()
            } else if (!isModelDownloaded) {
                onDownloadClick()
            }
        }.apply {
            isEnabled = !isModelDownloaded
            alpha = if (isModelDownloaded) 0.65f else 1f
        }

        root.addView(actionButton!!)

        addSpace(root, 16)

        root.addView(
            makeText(
                "После загрузки файл будет сохранён во внутренней памяти приложения.",
                12f,
                muted
            )
        )

        addSpace(root, 25)

        root.addView(makeButton("НАЗАД", onBackClick))

        return scroll
    }

    fun updateStatus(text: String) {
        statusView?.text = text
    }

    fun updateProgress(percent: Int) {
        progressBar?.progress = percent.coerceIn(0, 100)
    }

    fun updateSize(text: String) {
        sizeView?.text = text
    }

    fun updateAction(
        text: String,
        enabled: Boolean,
        alpha: Float = 1f
    ) {
        actionButton?.text = text
        actionButton?.isEnabled = enabled
        actionButton?.alpha = alpha
    }

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

    private fun makeButton(
        label: String,
        action: () -> Unit
    ): Button {

        return Button(context).apply {
            text = label
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(silver)
            gravity = Gravity.CENTER
            isAllCaps = false
            minHeight = dp(68)
            minimumHeight = dp(68)
            stateListAnimator = null

            backgroundTintList = ColorStateList.valueOf(panel)

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
            TextView(context),
            LinearLayout.LayoutParams(1, dp(height))
        )
    }
}
