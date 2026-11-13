package com.zeroproject.alice.ui
import android.content.Context
import android.widget.LinearLayout
import android.widget.ScrollView
import com.zeroproject.alice.settings.AliceSettingsStore
import com.zeroproject.alice.settings.AliceThemeResolver
import com.zeroproject.alice.settings.AliceThemePaletteFactory
object AliceScreenContainer {
    fun create(context: Context): Pair<ScrollView, LinearLayout> {
        val settings = AliceSettingsStore(context).load()
        val palette = AliceThemePaletteFactory.create(
            AliceThemeResolver.isDark(settings.appearance, context.resources.configuration), settings.accent)
        val scroll = ScrollView(context).apply { setBackgroundColor(palette.background) }
        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(palette.background)
            setPadding(AliceUiDimensions.dp(context, 16), AliceUiDimensions.dp(context, 24),
                AliceUiDimensions.dp(context, 16), AliceUiDimensions.dp(context, 24))
        }
        scroll.addView(content)
        return scroll to content
    }
}
