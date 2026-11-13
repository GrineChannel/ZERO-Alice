package com.zeroproject.alice.avatar
class AliceAvatarState(initial: AliceAvatarProfile = AliceAvatarProfile()) {
    @Volatile var profile: AliceAvatarProfile = initial
        private set
    @Synchronized fun updateEmotion(emotion: AliceEmotion) { profile = profile.copy(emotion = emotion) }
    @Synchronized fun updateStyle(style: AliceAvatarStyle) { profile = profile.copy(style = style) }
}
