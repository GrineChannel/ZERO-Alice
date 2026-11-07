
package com.zeroproject.alice

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private val navy = Color.rgb(11, 20, 38)
    private val red = Color.rgb(217, 54, 79)
    private val silver = Color.rgb(198, 205, 216)
    private val muted = Color.rgb(130, 145, 165)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setBackgroundColor(navy)
            setPadding(28, 32, 28, 24)
        }

        val logo = text("ZERO", 30f, silver, true)
        root.addView(logo)

        val studio = text("ZERO PROJECT", 11f, muted)
        root.addView(studio)

        addSpace(root, 45)

        val avatar = TextView(this).apply {
            text = "A"
            textSize = 76f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(red)
            gravity = Gravity.CENTER
            setBackgroundColor(Color.rgb(22, 34, 55))
        }

        root.addView(avatar, LinearLayout.LayoutParams(190, 190))

        addSpace(root, 22)

        root.addView(text("ALICE", 32f, silver, true))
        root.addView(text("ТВОЙ ЦИФРОВОЙ НАПАРНИК", 11f, muted))

        addSpace(root, 15)

        val status = text("●  СИСТЕМА АКТИВНА", 13f, red)
        root.addView(status)

        addSpace(root, 45)

        root.addView(actionButton("ОБЩЕНИЕ") {
            Toast.makeText(this, "Чат Элис будет подключён следующим этапом", Toast.LENGTH_SHORT).show()
        })

        addSpace(root, 12)

        root.addView(actionButton("ГОЛОС") {
            Toast.makeText(this, "Голосовой модуль пока разрабатывается", Toast.LENGTH_SHORT).show()
        })

        addSpace(root, 12)

        root.addView(actionButton("НАСТРОЙКИ") {
            Toast.makeText(this, "Настройки будут добавлены следующим этапом", Toast.LENGTH_SHORT).show()
        })

        val spacer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        root.addView(
            spacer,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        addSpace(root, 25)

        root.addView(text("ZERO PROJECT • 2026", 11f, muted))

        setContentView(root)
    }

    private fun text(
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
            if (bold) {
                typeface = Typeface.DEFAULT_BOLD
            }
        }
    }

    private fun actionButton(
        label: String,
        action: () -> Unit
    ): TextView {
        return TextView(this).apply {
            text = label
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(silver)
            gravity = Gravity.CENTER
            setBackgroundColor(Color.rgb(27, 42, 66))
            setPadding(16, 18, 16, 18)
            setOnClickListener { action() }

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                58
            )
        }
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
}
