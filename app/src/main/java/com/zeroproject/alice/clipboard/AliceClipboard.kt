
package com.zeroproject.alice.clipboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast

class AliceClipboard(context: Context) {

    private val appContext = context.applicationContext

    private val clipboardManager =
        appContext.getSystemService(
            Context.CLIPBOARD_SERVICE
        ) as ClipboardManager

    fun copyText(text: String): Boolean {
        if (text.isBlank()) return false

        return try {
            val clip = ClipData.newPlainText(
                "ZERO: Alice",
                text
            )

            clipboardManager.setPrimaryClip(clip)

            Toast.makeText(
                appContext,
                "Текст скопирован",
                Toast.LENGTH_SHORT
            ).show()

            true

        } catch (error: Exception) {
            false
        }
    }

    fun getCopiedText(): String? {
        return try {
            if (!clipboardManager.hasPrimaryClip()) {
                return null
            }

            clipboardManager.primaryClip
                ?.getItemAt(0)
                ?.coerceToText(appContext)
                ?.toString()

        } catch (error: Exception) {
            null
        }
    }
}
