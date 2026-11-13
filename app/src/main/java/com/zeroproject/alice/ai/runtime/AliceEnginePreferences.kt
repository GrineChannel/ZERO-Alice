package com.zeroproject.alice.ai.runtime

import android.content.Context

/** Stores only engine selection, never prompts or private conversations. */
class AliceEnginePreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        "alice_engine_preferences", Context.MODE_PRIVATE
    )

    fun selectedId(): String {
        val saved = prefs.getString("selected_engine", AliceEngineCatalog.GEMMA)
            ?: AliceEngineCatalog.GEMMA
        return AliceEngineCatalog.find(saved)?.takeIf { it.available }?.id
            ?: AliceEngineCatalog.GEMMA
    }

    fun select(id: String): Boolean {
        val descriptor = AliceEngineCatalog.find(id) ?: return false
        if (!descriptor.available) return false
        return prefs.edit().putString("selected_engine", id).commit()
    }
}
