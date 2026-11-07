
package com.zeroproject.alice

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.content.res.ColorStateList
import android.widget.Toast

class MainActivity : Activity() {

    private val navy = Color.rgb(11, 20, 38)
    private val panel = Color.rgb(27, 42, 66)
    private val red = Color.rgb(217, 54, 79)
    private val silver = Color.rgb(198, 205, 216)
    private val muted = Color.rgb(145, 158, 180)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val scroll = ScrollView(this).apply {
            fillViewport = true
            setBackgroundColor(navy)
            isVerticalScrollBarEnabled = false
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(24, 36, 24, 32)
            setBackgroundColor(navy)
        }

        scroll.addView(
            root,
            ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        root.addView(
            makeText("ZERO", 32f, silver, true),
            fullWidth()
        )

        root.addView(
            makeText("ZERO PROJECT", 12f, muted),
            fullWidth()
        )

        addSpace(root, 35)

        val avatar = TextView(this).apply {
            text = "A"
            textSize = 76f
            includeFontPadding = true
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(red)
            background = rounded(panel, red, 3, 28)
        }

        root.addView(
            avatar,
            LinearLayout.LayoutParams(190, 190)
        )

        addSpace(root, 24)

        root.addView(
            makeText("ALICE", 32f, silver, true),
            fullWidth()
        )

        root.addView(
            makeText("ТВОЙ ЦИФРОВОЙ НАПАРНИК", 12f, muted),
            fullWidth()
        )

        addSpace(root, 16)

        root.addView(
            makeText("●  СИСТЕМА АКТИВНА", 14f, red, true),
            fullWidth()
        )

        addSpace(root, 35)

        root.addView(makeButton("ОБЩЕНИЕ") {
            showMessage("Чат Элис будет подключён следующим этапом")
        })

        addSpace(root, 14)

        root.addView(makeButton("ГОЛОС") {
            showMessage("Голосовой модуль пока разрабатывается")
        })

        addSpace(root, 14)

        root.addView(makeButton("НАСТРОЙКИ") {
            showMessage("Настройки будут добавлены следующим этапом")
        })

        addSpace(root, 35)

        root.addView(
            makeText("ZERO PROJECT • 2026", 11f, muted),
            fullWidth()
        )

        setContentView(scroll)
    }

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
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(silver)
            gravity = Gravity.CENTER
            isAllCaps = false
            minHeight = 64
            minimumHeight = 64
            stateListAnimator = null
            backgroundTintList = ColorStateList.valueOf(panel)
            setPadding(16, 12, 16, 12)
            setOnClickListener { action() }

            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                64
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
            setCornerRadius(radius.toFloat())
            setStroke(strokeWidth, strokeColor)
        }
    }

    private fun fullWidth(): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun addSpace(
        layout: LinearLayout,
        height: Int
    ) {
        val space = TextView(this)

        layout.addView(
            space,
            LinearLayout.LayoutParams(1, height)
        )
    }

    private fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
