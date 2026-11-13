package com.zeroproject.alice.screens
import android.content.Context
import android.view.View
import android.widget.LinearLayout
import com.zeroproject.alice.settings.AliceSettingsStore
import com.zeroproject.alice.ui.AliceScreenContainer
import com.zeroproject.alice.ui.AliceSectionHeader
import com.zeroproject.alice.ui.AliceToggleRow
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
        content.addView(AliceTextFactory.subtitle(context,
            "Изменение параметров сохранится; подключение к остальным экранам будет добавлено позже."))
        return scroll
    }
}
