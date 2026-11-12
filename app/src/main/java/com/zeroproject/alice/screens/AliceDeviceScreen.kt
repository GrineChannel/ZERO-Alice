package com.zeroproject.alice.screens

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.zeroproject.alice.device.AliceDeviceActions

class AliceDeviceScreen(
    private val context: Context,
    private val onBack: () -> Unit
) {
    private val actions = AliceDeviceActions(context)

    fun create(): View {
        val density = context.resources.displayMetrics.density
        fun dp(value: Int) = (value * density).toInt()

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(11, 20, 38))
            setPadding(dp(20), dp(28), dp(20), dp(20))
        }
        val scroll = ScrollView(context)
        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }
        val status = TextView(context).apply {
            text = "Действия выполняются только после нажатия кнопки."
            textSize = 14f
            setTextColor(Color.LTGRAY)
            setPadding(0, dp(12), 0, dp(20))
        }
        fun button(label: String, action: () -> Unit): Button =
            Button(context).apply {
                text = label
                isAllCaps = false
                setOnClickListener { action() }
            }

        content.addView(button("‹ Назад", onBack))
        content.addView(TextView(context).apply {
            text = "УПРАВЛЕНИЕ ТЕЛЕФОНОМ"
            textSize = 22f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setPadding(0, dp(20), 0, dp(12))
        })
        content.addView(status)
        actions.availableActions().forEach { item ->
            content.addView(button(item.title) {
                if (!actions.execute(item.id)) {
                    status.text = "Не удалось открыть: ${item.title}"
                }
            })
        }
        scroll.addView(content)
        root.addView(scroll)
        return root
    }
}
