package com.zeroproject.alice.ui
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.widget.LinearLayout
object AliceCardFactory {
    fun create(context: Context): LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(AliceUiDimensions.dp(context, 16), AliceUiDimensions.dp(context, 12),
            AliceUiDimensions.dp(context, 16), AliceUiDimensions.dp(context, 12))
        background = GradientDrawable().apply {
            setColor(AlicePalette.SURFACE)
            cornerRadius = AliceUiDimensions.dp(context, 16).toFloat()
        }
    }
}
