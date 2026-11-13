package com.zeroproject.alice.settings
data class AliceAppSettings(val appearance: AliceAppearance = AliceAppearance.DARK, val accent: AliceAccent = AliceAccent.RED, val voiceReplies: Boolean = false, val animatedAvatar: Boolean = true, val saveChatHistory: Boolean = true)
