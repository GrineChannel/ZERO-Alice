package com.zeroproject.alice.ui
import android.content.Context
import android.widget.LinearLayout
import android.widget.Switch
object AliceToggleRow {
    @Suppress("DEPRECATION")
    fun create(context: Context, label: String, checked: Boolean, onChange: (Boolean) -> Unit): Switch =
        Switch(context).apply {
            text = label
            isChecked = checked
            setTextColor(AlicePalette.SILVER)
            setOnCheckedChangeListener { _, value -> onChange(value) }
        }
}
