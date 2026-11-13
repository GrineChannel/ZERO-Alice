package com.zeroproject.alice.ui
import android.content.Context
import android.graphics.Typeface
import android.widget.TextView
import com.zeroproject.alice.settings.AliceSettingsStore
import com.zeroproject.alice.settings.AliceThemeResolver
import com.zeroproject.alice.settings.AliceThemePaletteFactory
object AliceTextFactory {
    private fun color(context: Context): Int {
        val settings = AliceSettingsStore(context).load()
        return AliceThemePaletteFactory.create(
            AliceThemeResolver.isDark(settings.appearance, context.resources.configuration), settings.accent).text
    }
    fun title(context: Context, value: String): TextView = TextView(context).apply {
        text = value
        textSize = 20f
        setTextColor(color(context))
        setTypeface(typeface, Typeface.BOLD)
    }
    fun subtitle(context: Context, value: String): TextView = TextView(context).apply {
        text = value
        textSize = 14f
        setTextColor(color(context))
        alpha = 0.85f
    }
}
