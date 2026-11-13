package com.zeroproject.alice.ui
import android.content.Context
object AliceUiDimensions {
    fun dp(context: Context, value: Int): Int =
        (value * context.resources.displayMetrics.density + 0.5f).toInt()
}
