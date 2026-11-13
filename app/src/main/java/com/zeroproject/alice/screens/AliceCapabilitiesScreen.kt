package com.zeroproject.alice.screens
import android.content.Context
import android.view.View
import com.zeroproject.alice.app.AliceCapabilityCatalog
import com.zeroproject.alice.ai.runtime.AliceEngineCatalog
import com.zeroproject.alice.ai.runtime.AliceEnginePreferences
import com.zeroproject.alice.ui.AliceChoiceRow
import com.zeroproject.alice.ui.AliceScreenContainer
import com.zeroproject.alice.ui.AliceSectionHeader
import com.zeroproject.alice.ui.AliceTextFactory
import com.zeroproject.alice.ui.AliceStatusLabels
class AliceCapabilitiesScreen(private val context: Context, private val onBack: () -> Unit = {}) {
    fun createView(): View {
        val (scroll, content) = AliceScreenContainer.create(context)
        content.addView(AliceChoiceRow.create(context, "← НАЗАД") { onBack() })
        AliceSectionHeader.add(content, "Возможности Элис", "Базовый план модулей")
        AliceCapabilityCatalog.baseline().forEach { capability ->
            content.addView(AliceTextFactory.subtitle(context,
                "${capability.feature.name}: ${AliceStatusLabels.label(capability.status)}"))
        }
        AliceSectionHeader.add(content, "Движки ИИ", "Состояние подключения")
        val selected = AliceEnginePreferences(context).selectedId()
        AliceEngineCatalog.entries().forEach { engine ->
            val status = if (!engine.available) "планируется"
                else if (engine.id == selected) "выбран" else "доступен"
            content.addView(AliceTextFactory.subtitle(context, "${engine.title}: $status"))
            content.addView(AliceTextFactory.subtitle(context, engine.description))
        }
        return scroll
    }
}
