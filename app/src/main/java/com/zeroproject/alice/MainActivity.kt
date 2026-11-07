
package com.zeroproject.alice

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.rgb(11, 20, 38))
            setPadding(32, 32, 32, 32)
        }

        val title = TextView(this).apply {
            text = "ZERO"
            textSize = 38f
            setTextColor(Color.rgb(198, 205, 216))
            gravity = Gravity.CENTER
        }

        val subtitle = TextView(this).apply {
            text = "ALICE"
            textSize = 25f
            setTextColor(Color.rgb(217, 54, 79))
            gravity = Gravity.CENTER
        }

        val status = TextView(this).apply {
            text = "Система инициализирована"
            textSize = 15f
            setTextColor(Color.rgb(170, 180, 194))
            gravity = Gravity.CENTER
        }

        root.addView(title)
        root.addView(subtitle)
        root.addView(status)

        setContentView(root)
    }
}
