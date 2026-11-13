package com.zeroproject.alice.avatar
import android.content.Context
import android.widget.LinearLayout
object AliceAvatarPreviewFactory {
    fun create(context: Context, sizeDp: Int = 180): AliceAvatarView {
        val px = (sizeDp * context.resources.displayMetrics.density).toInt()
        return AliceAvatarView(context).apply {
            layoutParams = LinearLayout.LayoutParams(px, px)
            contentDescription = "Схематичный аватар Элис"
        }
    }
}
