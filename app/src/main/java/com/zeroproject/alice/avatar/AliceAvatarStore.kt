package com.zeroproject.alice.avatar
import android.content.Context
class AliceAvatarStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("alice_avatar", Context.MODE_PRIVATE)
    fun load(): AliceAvatarProfile = AliceAvatarProfile(
        style = AliceAvatarStyle.entries.firstOrNull { it.name == prefs.getString("style", null) } ?: AliceAvatarStyle.CANONICAL,
        animationsEnabled = prefs.getBoolean("animations", true)
    )
    fun save(profile: AliceAvatarProfile): Boolean = prefs.edit()
        .putString("style", profile.style.name)
        .putBoolean("animations", profile.animationsEnabled).commit()
}
