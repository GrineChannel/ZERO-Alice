package com.zeroproject.alice.avatar

import android.content.Context
import android.graphics.BitmapFactory
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout

object AliceAvatarPreviewFactory {
    fun create(context: Context, sizeDp: Int = 180): View {
        val px = (sizeDp * context.resources.displayMetrics.density).toInt()
        val layout = LinearLayout.LayoutParams(px, px)
        val artwork = AliceAvatarArtworkStore(context).existingFile()
        if (artwork != null) {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(artwork.absolutePath, bounds)
            val maxDimension = maxOf(bounds.outWidth, bounds.outHeight)
            var sample = 1
            while (maxDimension / sample > px * 2 && sample < 128) sample *= 2
            val bitmap = BitmapFactory.decodeFile(artwork.absolutePath,
                BitmapFactory.Options().apply { inSampleSize = sample })
            if (bitmap != null) {
                return ImageView(context).apply {
                    layoutParams = layout
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    setImageBitmap(bitmap)
                    contentDescription = "Выбранное изображение Элис"
                }
            }
        }
        return AliceAvatarView(context).apply {
            layoutParams = layout
            contentDescription = "Временный схематичный аватар Элис"
        }
    }
}
