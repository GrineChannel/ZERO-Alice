package com.zeroproject.alice.screens

import android.content.Context
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import com.zeroproject.alice.settings.AliceSettingsStore
import com.zeroproject.alice.settings.AliceAppearance
import com.zeroproject.alice.settings.AliceAccent
import com.zeroproject.alice.ui.AliceScreenContainer
import com.zeroproject.alice.ui.AliceSectionHeader
import com.zeroproject.alice.ui.AliceToggleRow
import com.zeroproject.alice.ui.AliceChoiceRow
import com.zeroproject.alice.ui.AliceTextFactory

class AliceSettingsScreen(private val context: Context) {
    private val store = AliceSettingsStore(context)

    fun createView(): View {
        val (scroll, content) = AliceScreenContainer.create(context)
        AliceSectionHeader.add(content, "Настройки Элис", "Локальные параметры приложения")
        val settings = store.load()
        content.addView(AliceToggleRow.create(context, "Анимация аватара", settings.animatedAvatar) {
            store.save(store.load().copy(animatedAvatar = it))
        })
        content.addView(AliceToggleRow.create(context, "Голосовые ответы", settings.voiceReplies) {
            store.save(store.load().copy(voiceReplies = it))
        })
        content.addView(AliceTextFactory.title(context, "Оформление"))
        val themeLabel = AliceTextFactory.subtitle(context, "Тема: ${settings.appearance.name}")
        content.addView(themeLabel)
        content.addView(AliceChoiceRow.create(context, "СМЕНИТЬ ТЕМУ") {
            val next = when (store.load().appearance) {
                AliceAppearance.DARK -> AliceAppearance.LIGHT
                AliceAppearance.LIGHT -> AliceAppearance.SYSTEM
                AliceAppearance.SYSTEM -> AliceAppearance.DARK
            }
            store.save(store.load().copy(appearance = next))
            themeLabel.text = "Тема: ${next.name}"
            Toast.makeText(context, "Выбор темы сохранён", Toast.LENGTH_SHORT).show()
        })
        val accentLabel = AliceTextFactory.subtitle(context, "Акцент: ${settings.accent.name}")
        content.addView(accentLabel)
        content.addView(AliceChoiceRow.create(context, "СМЕНИТЬ ЦВЕТ") {
            val next = when (store.load().accent) {
                AliceAccent.RED -> AliceAccent.BLUE
                AliceAccent.BLUE -> AliceAccent.SILVER
                AliceAccent.SILVER -> AliceAccent.RED
            }
            store.save(store.load().copy(accent = next))
            accentLabel.text = "Акцент: ${next.name}"
        })
        content.addView(AliceTextFactory.subtitle(context,
            "Выбранное оформление сохранено. Применение темы ко всем экранам будет добавлено следующим этапом."))
        return scroll
    }
}
