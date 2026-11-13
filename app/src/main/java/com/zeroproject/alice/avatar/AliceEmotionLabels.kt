package com.zeroproject.alice.avatar
object AliceEmotionLabels {
    fun label(emotion: AliceEmotion): String = when (emotion) {
        AliceEmotion.NEUTRAL -> "Спокойна"
        AliceEmotion.HAPPY -> "Рада"
        AliceEmotion.CURIOUS -> "Заинтересована"
        AliceEmotion.THINKING -> "Думает"
        AliceEmotion.CONCERNED -> "Обеспокоена"
        AliceEmotion.EXCITED -> "Воодушевлена"
    }
}
