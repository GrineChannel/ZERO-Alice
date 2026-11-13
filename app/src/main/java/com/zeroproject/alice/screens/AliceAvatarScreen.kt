package com.zeroproject.alice.screens

import android.content.Context
import android.view.View
import android.widget.Toast
import com.zeroproject.alice.avatar.AliceAvatarDescription
import com.zeroproject.alice.avatar.AliceAvatarStore
import com.zeroproject.alice.avatar.AliceAvatarPreviewFactory
import com.zeroproject.alice.avatar.AliceAvatarAccessibility
import com.zeroproject.alice.avatar.AliceAvatarStyle
import com.zeroproject.alice.ui.AliceChoiceRow
import com.zeroproject.alice.ui.AliceScreenContainer
import com.zeroproject.alice.ui.AliceSectionHeader
import com.zeroproject.alice.ui.AliceTextFactory
import com.zeroproject.alice.ui.AliceToggleRow

class AliceAvatarScreen(private val context: Context, private val onBack: () -> Unit = {}) {
    private val store = AliceAvatarStore(context)

    fun createView(): View {
        val (scroll, content) = AliceScreenContainer.create(context)
        content.addView(AliceChoiceRow.create(context, "← НАЗАД") { onBack() })
        AliceSectionHeader.add(content, "Образ Элис", "Настройки внешности")
        val current = store.load()
        content.addView(AliceAvatarPreviewFactory.create(context).apply {
            contentDescription = AliceAvatarAccessibility.description(current)
        })
        val description = AliceTextFactory.subtitle(context, AliceAvatarDescription.describe(current.style))
        content.addView(description)
        content.addView(AliceToggleRow.create(context, "Анимации", current.animationsEnabled) {
            store.save(store.load().copy(animationsEnabled = it))
        })
        content.addView(AliceChoiceRow.create(context, "КАНОНИЧЕСКИЙ ОБРАЗ") {
            store.save(store.load().copy(style = AliceAvatarStyle.CANONICAL))
            description.text = AliceAvatarDescription.describe(AliceAvatarStyle.CANONICAL)
            Toast.makeText(context, "Канонический образ выбран", Toast.LENGTH_SHORT).show()
        })
        content.addView(AliceTextFactory.subtitle(context,
            "Другие образы и отображение иллюстрации появятся позже."))
        return scroll
    }
}
