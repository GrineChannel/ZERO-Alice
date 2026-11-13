package com.zeroproject.alice.screens
import android.content.Context
import android.view.View
import com.zeroproject.alice.voice.AliceVoicePreferences
import com.zeroproject.alice.voice.AliceVoiceModeLabels
import com.zeroproject.alice.ui.AliceScreenContainer
import com.zeroproject.alice.ui.AliceSectionHeader
import com.zeroproject.alice.ui.AliceTextFactory
class AliceVoiceSettingsScreen(private val context: Context) {
    fun createView(): View {
        val (scroll, content) = AliceScreenContainer.create(context)
        AliceSectionHeader.add(content, "Голос Элис", "Настройки речевого взаимодействия")
        content.addView(AliceTextFactory.subtitle(context,
            "Режим: " + AliceVoiceModeLabels.label(AliceVoicePreferences(context).mode())))
        content.addView(AliceTextFactory.subtitle(context,
            "Фоновое прослушивание не включено."))
        return scroll
    }
}
