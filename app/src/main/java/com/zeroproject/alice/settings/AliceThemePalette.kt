package com.zeroproject.alice.settings
data class AliceThemePalette(val background: Int, val surface: Int, val text: Int, val accent: Int)
object AliceThemePaletteFactory {
    fun create(isDark: Boolean, accent: AliceAccent): AliceThemePalette =
        if (isDark) AliceThemePalette(0xFF0B1220.toInt(), 0xFF172235.toInt(),
            0xFFCCD4DE.toInt(), accent.argb)
        else AliceThemePalette(0xFFF5F7FA.toInt(), 0xFFE4EAF1.toInt(),
            0xFF172235.toInt(), accent.argb)
}
