package com.zeroproject.alice.ui
import android.content.Context
import android.widget.Button
object AliceChoiceRow {
    fun create(context: Context, label: String, onClick: () -> Unit): Button =
        Button(context).apply { text = label; setOnClickListener { onClick() } }
}
