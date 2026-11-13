package com.zeroproject.alice.ui
import android.content.Context
import android.widget.Switch
import com.zeroproject.alice.settings.AliceSettingsStore
import com.zeroproject.alice.settings.AliceThemeResolver
import com.zeroproject.alice.settings.AliceThemePaletteFactory
object AliceToggleRow {
    @Suppress("DEPRECATION")
    fun create(context: Context, label: String, checked: Boolean, onChange: (Boolean) -> Unit): Switch =
        Switch(context).apply {
            val settings = AliceSettingsStore(context).load()
            val palette = AliceThemePaletteFactory.create(
                AliceThemeResolver.isDark(settings.appearance, context.resources.configuration), settings.accent)
            text = label
            isChecked = checked
            setTextColor(palette.text)
            setOnCheckedChangeListener { _, value -> onChange(value) }
        }
}
