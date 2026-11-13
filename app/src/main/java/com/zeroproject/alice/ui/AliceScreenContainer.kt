package com.zeroproject.alice.ui
import android.content.Context
import android.widget.LinearLayout
import android.widget.ScrollView
object AliceScreenContainer {
    fun create(context: Context): Pair<ScrollView, LinearLayout> {
        val scroll = ScrollView(context)
        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(AlicePalette.BACKGROUND)
            setPadding(AliceUiDimensions.dp(context, 16), AliceUiDimensions.dp(context, 24),
                AliceUiDimensions.dp(context, 16), AliceUiDimensions.dp(context, 24))
        }
        scroll.addView(content)
        return scroll to content
    }
}
