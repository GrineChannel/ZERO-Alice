
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

class AliceHomeScreen(
    private val context: Context,
    private val onChatClick: () -> Unit,
    private val onVoiceClick: () -> Unit,
    private val onModelClick: () -> Unit,
    private val onSettingsClick: () -> Unit,
    private val onEngineerClick: () -> Unit = {}
) {

    private val navy = Color.rgb(11, 20, 38)
    private val panel = Color.rgb(27, 42, 66)
    private val red = Color.rgb(217, 54, 79)
    private val silver = Color.rgb(198, 205, 216)
    private val muted = Color.rgb(145, 158, 180)

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

        val avatar = TextView(context).apply {
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

        root.addView(makeButton("ОБЩЕНИЕ", onChatClick))

        addSpace(root, 14)

        root.addView(makeButton("ГОЛОС", onVoiceClick))

        addSpace(root, 14)

        root.addView(makeButton("ИНЖЕНЕРНАЯ ЛАБОРАТОРИЯ", onEngineerClick))

        addSpace(root, 14)

        root.addView(makeButton("МОДЕЛЬ ИИ", onModelClick))

        addSpace(root, 14)

        root.addView(makeButton("НАСТРОЙКИ", onSettingsClick))

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
