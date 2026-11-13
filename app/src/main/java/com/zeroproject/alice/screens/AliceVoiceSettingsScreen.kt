package com.zeroproject.alice.screens

import android.content.Context
import android.view.View
import android.widget.Toast
import com.zeroproject.alice.voice.AliceVoicePreferences
import com.zeroproject.alice.voice.AliceVoiceMode
import com.zeroproject.alice.voice.AliceVoiceModeLabels
import com.zeroproject.alice.ui.AliceChoiceRow
import com.zeroproject.alice.ui.AliceScreenContainer
import com.zeroproject.alice.ui.AliceSectionHeader
import com.zeroproject.alice.ui.AliceTextFactory

class AliceVoiceSettingsScreen(private val context: Context, private val onBack: () -> Unit = {}) {
    private val preferences = AliceVoicePreferences(context)

    fun createView(): View {
        val (scroll, content) = AliceScreenContainer.create(context)
        content.addView(AliceChoiceRow.create(context, "← НАЗАД") { onBack() })
        AliceSectionHeader.add(content, "Голос Элис", "Настройки речевого взаимодействия")
        val modeLabel = AliceTextFactory.subtitle(context,
            "Режим: " + AliceVoiceModeLabels.label(preferences.mode()))
        content.addView(modeLabel)
        content.addView(AliceChoiceRow.create(context, "ВКЛЮЧИТЬ НАЖАТИЕ ДЛЯ РАЗГОВОРА") {
            preferences.setMode(AliceVoiceMode.PUSH_TO_TALK)
            modeLabel.text = "Режим: " + AliceVoiceModeLabels.label(preferences.mode())
            Toast.makeText(context, "Режим сохранён", Toast.LENGTH_SHORT).show()
        })
        content.addView(AliceChoiceRow.create(context, "ВЫКЛЮЧИТЬ ГОЛОСОВОЙ ВВОД") {
            preferences.setMode(AliceVoiceMode.OFF)
            modeLabel.text = "Режим: " + AliceVoiceModeLabels.label(preferences.mode())
            Toast.makeText(context, "Режим сохранён", Toast.LENGTH_SHORT).show()
        })
        val rateLabel = AliceTextFactory.subtitle(context,
            "Скорость речи: " + when (preferences.speechRate()) {
                0.85f -> "медленная"
                1.15f -> "быстрая"
                else -> "обычная"
            })
        content.addView(rateLabel)
        content.addView(AliceChoiceRow.create(context, "МЕДЛЕННАЯ РЕЧЬ") {
            preferences.setSpeechRate(0.85f)
            rateLabel.text = "Скорость речи: медленная"
        })
        content.addView(AliceChoiceRow.create(context, "ОБЫЧНАЯ РЕЧЬ") {
            preferences.setSpeechRate(1.0f)
            rateLabel.text = "Скорость речи: обычная"
        })
        content.addView(AliceChoiceRow.create(context, "БЫСТРАЯ РЕЧЬ") {
            preferences.setSpeechRate(1.15f)
            rateLabel.text = "Скорость речи: быстрая"
        })
        content.addView(AliceTextFactory.subtitle(context,
            "Режим без рук пока не реализован. Настройка будет подключена к голосовой кнопке отдельно."))
        return scroll
    }
}
