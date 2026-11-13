package com.zeroproject.alice.avatar
object AliceAvatarAccessibility {
    fun description(profile: AliceAvatarProfile): String =
        "Элис. ${AliceAvatarDescription.describe(profile.style)}. " +
        "Эмоция: ${AliceEmotionLabels.label(profile.emotion)}"
}
