package com.zeroproject.alice.app
object AliceDestinationCatalog {
    fun label(destination: AliceDestination): String = when (destination) {
        AliceDestination.HOME -> "Главная"
        AliceDestination.CHAT -> "Чат"
        AliceDestination.MODEL -> "Модель ИИ"
        AliceDestination.AVATAR -> "Образ Элис"
        AliceDestination.VOICE -> "Голос"
        AliceDestination.SETTINGS -> "Настройки"
        AliceDestination.FILES -> "Файлы"
        AliceDestination.TASKS -> "Задачи"
        AliceDestination.DEVICE -> "Устройство"
        AliceDestination.MINECRAFT -> "Minecraft"
    }
}
