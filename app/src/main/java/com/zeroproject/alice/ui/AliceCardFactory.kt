package com.zeroproject.alice.ui
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.widget.LinearLayout
import com.zeroproject.alice.settings.AliceSettingsStore
import com.zeroproject.alice.settings.AliceThemeResolver
import com.zeroproject.alice.settings.AliceThemePaletteFactory
object AliceCardFactory {
    fun create(context: Context): LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(AliceUiDimensions.dp(context, 16), AliceUiDimensions.dp(context, 12),
            AliceUiDimensions.dp(context, 16), AliceUiDimensions.dp(context, 12))
        val settings = AliceSettingsStore(context).load()
        val palette = AliceThemePaletteFactory.create(
            AliceThemeResolver.isDark(settings.appearance, context.resources.configuration), settings.accent)
        background = GradientDrawable().apply {
            setColor(palette.surface)
            cornerRadius = AliceUiDimensions.dp(context, 16).toFloat()
        }
    }
}
