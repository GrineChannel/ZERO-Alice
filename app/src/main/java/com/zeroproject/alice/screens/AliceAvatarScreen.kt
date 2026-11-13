package com.zeroproject.alice.screens
import android.content.Context
import android.view.View
import com.zeroproject.alice.avatar.AliceAvatarDescription
import com.zeroproject.alice.avatar.AliceAvatarStore
import com.zeroproject.alice.ui.AliceScreenContainer
import com.zeroproject.alice.ui.AliceSectionHeader
import com.zeroproject.alice.ui.AliceTextFactory
class AliceAvatarScreen(private val context: Context) {
    fun createView(): View {
        val (scroll, content) = AliceScreenContainer.create(context)
        val profile = AliceAvatarStore(context).load()
        AliceSectionHeader.add(content, "Образ Элис", "Канонический образ сохранён")
        content.addView(AliceTextFactory.subtitle(context, AliceAvatarDescription.describe(profile.style)))
        content.addView(AliceTextFactory.subtitle(context,
            "Отображение иллюстрации и смена образов будут подключены отдельно."))
        return scroll
    }
}
