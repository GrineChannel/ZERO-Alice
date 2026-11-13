package com.zeroproject.alice.screens
import android.content.Context
import android.view.View
import com.zeroproject.alice.app.AliceCapabilityCatalog
import com.zeroproject.alice.ui.AliceScreenContainer
import com.zeroproject.alice.ui.AliceSectionHeader
import com.zeroproject.alice.ui.AliceTextFactory
import com.zeroproject.alice.ui.AliceStatusLabels
class AliceCapabilitiesScreen(private val context: Context) {
    fun createView(): View {
        val (scroll, content) = AliceScreenContainer.create(context)
        AliceSectionHeader.add(content, "Возможности Элис", "Базовый план модулей")
        AliceCapabilityCatalog.baseline().forEach { capability ->
            content.addView(AliceTextFactory.subtitle(context,
                "${capability.feature.name}: ${AliceStatusLabels.label(capability.status)}"))
        }
        return scroll
    }
}
