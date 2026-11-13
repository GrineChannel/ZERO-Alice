package com.zeroproject.alice.ui
import android.content.Context
import android.graphics.Typeface
import android.widget.TextView
object AliceTextFactory {
    fun title(context: Context, value: String): TextView = TextView(context).apply {
        text = value
        textSize = 20f
        setTextColor(AlicePalette.SILVER)
        setTypeface(typeface, Typeface.BOLD)
    }
    fun subtitle(context: Context, value: String): TextView = TextView(context).apply {
        text = value
        textSize = 14f
        setTextColor(AlicePalette.MUTED)
    }
}
