
package com.zeroproject.alice.screens

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.zeroproject.alice.settings.AliceSettingsStore
import com.zeroproject.alice.settings.AliceThemeResolver
import com.zeroproject.alice.settings.AliceThemePaletteFactory
import com.zeroproject.alice.avatar.AliceAvatarPreviewFactory

class AliceHomeScreen(
    private val context: Context,
    private val onChatClick: () -> Unit,
    private val onVoiceClick: () -> Unit,
    private val onModelClick: () -> Unit,
    private val onSettingsClick: () -> Unit,
    private val onEngineerClick: () -> Unit = {},
    private val onDeviceClick: () -> Unit = {},
    private val onAvatarClick: () -> Unit = {},
    private val onVoiceSettingsClick: () -> Unit = {},
    private val onCapabilitiesClick: () -> Unit = {}
) {

    private val appearance = AliceSettingsStore(context).load()
    private val theme = AliceThemePaletteFactory.create(
        AliceThemeResolver.isDark(appearance.appearance, context.resources.configuration),
        appearance.accent
    )
    private val navy = theme.background
    private val panel = theme.surface
    private val red = theme.accent
    private val silver = theme.text
    private val muted = if (AliceThemeResolver.isDark(appearance.appearance, context.resources.configuration))
        Color.rgb(158, 172, 195) else Color.rgb(77, 92, 112)

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

        val logoPanel = LinearLayout(context).apply {
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

        // Temporary vector preview; approved artwork will replace it later.
        root.addView(AliceAvatarPreviewFactory.create(context, 200).apply {
            isClickable = true
            isFocusable = true
            contentDescription = "Открыть настройки образа Элис"
            setOnClickListener { onAvatarClick() }
        })

        addSpace(root, 24)

        root.addView(makeText("ALICE", 34f, silver, true))
        root.addView(makeText("ТВОЙ ЦИФРОВОЙ НАПАРНИК", 12f, muted))

        addSpace(root, 16)

        root.addView(
            makeText("●  СИСТЕМА АКТИВНА", 14f, red, true)
        )

        addSpace(root, 35)

        root.addView(makeButton("ОБЩЕНИЕ", onChatClick))

        addSpace(root, 14)

        root.addView(makeButton("ГОЛОС", onVoiceClick))

        addSpace(root, 14)

        root.addView(makeButton("ИНЖЕНЕРНАЯ ЛАБОРАТОРИЯ", onEngineerClick))

        addSpace(root, 14)

        root.addView(makeButton("УПРАВЛЕНИЕ ТЕЛЕФОНОМ", onDeviceClick))

        addSpace(root, 14)

        root.addView(makeButton("МОДЕЛЬ ИИ", onModelClick))

        addSpace(root, 14)

        root.addView(makeButton("НАСТРОЙКИ", onSettingsClick))

        addSpace(root, 14)
        root.addView(makeButton("ОБРАЗ ЭЛИС", onAvatarClick))

        addSpace(root, 14)
        root.addView(makeButton("НАСТРОЙКИ ГОЛОСА", onVoiceSettingsClick))

        addSpace(root, 14)
        root.addView(makeButton("ВОЗМОЖНОСТИ ЭЛИС", onCapabilitiesClick))

        addSpace(root, 35)

        root.addView(makeText("ZERO PROJECT • 2026", 11f, muted))

        return scroll
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
            TextView(context),
            LinearLayout.LayoutParams(1, dp(height))
        )
    }
}
