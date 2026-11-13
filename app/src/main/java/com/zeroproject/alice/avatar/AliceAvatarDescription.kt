package com.zeroproject.alice.avatar
object AliceAvatarDescription {
    const val CANONICAL = "Серебряные волосы в высоком хвосте, красные глаза, " +
        "чёрно-синий футуристический костюм с серебряными и красными акцентами"
    fun describe(style: AliceAvatarStyle): String = when (style) {
        AliceAvatarStyle.CANONICAL -> CANONICAL
        AliceAvatarStyle.CASUAL -> "Повседневный образ (в разработке)"
        AliceAvatarStyle.FORMAL -> "Официальный образ (в разработке)"
        AliceAvatarStyle.CUSTOM -> "Пользовательский образ (в разработке)"
    }
}
