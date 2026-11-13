package com.zeroproject.alice.ui
import com.zeroproject.alice.app.AliceCapabilityStatus
object AliceStatusLabels {
    fun label(status: AliceCapabilityStatus): String = when (status) {
        AliceCapabilityStatus.READY -> "Основа готова"
        AliceCapabilityStatus.NEEDS_SETUP -> "Требуется настройка"
        AliceCapabilityStatus.PLANNED -> "В разработке"
        AliceCapabilityStatus.UNAVAILABLE -> "Недоступно"
    }
}
