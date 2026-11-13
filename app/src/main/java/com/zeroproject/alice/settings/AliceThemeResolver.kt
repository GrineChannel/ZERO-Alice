package com.zeroproject.alice.settings
import android.content.res.Configuration
object AliceThemeResolver {
    fun isDark(appearance: AliceAppearance, configuration: Configuration): Boolean = when (appearance) {
        AliceAppearance.DARK -> true
        AliceAppearance.LIGHT -> false
        AliceAppearance.SYSTEM ->
            configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    }
}
