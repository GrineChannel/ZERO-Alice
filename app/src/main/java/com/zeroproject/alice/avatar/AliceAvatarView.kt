package com.zeroproject.alice.avatar
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.View
import kotlin.math.min
/**
 * Lightweight symbolic avatar until the approved character artwork is bundled.
 * This is not a replacement for the canonical illustration.
 */
class AliceAvatarView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    var emotion: AliceEmotion = AliceEmotion.NEUTRAL
        set(value) { field = value; invalidate() }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val size = min(width, height).toFloat()
        if (size <= 0f) return
        val cx = width / 2f
        val cy = height / 2f
        paint.color = AliceAvatarPalette.SUIT
        canvas.drawCircle(cx, cy, size * 0.47f, paint)
        paint.color = AliceAvatarPalette.HAIR
        canvas.drawOval(cx - size * 0.24f, cy - size * 0.32f,
            cx + size * 0.24f, cy + size * 0.32f, paint)
        paint.color = AliceAvatarPalette.SKIN
        canvas.drawOval(cx - size * 0.18f, cy - size * 0.20f,
            cx + size * 0.18f, cy + size * 0.22f, paint)
        paint.color = AliceAvatarPalette.EYES
        canvas.drawCircle(cx - size * 0.075f, cy - size * 0.015f, size * 0.025f, paint)
        canvas.drawCircle(cx + size * 0.075f, cy - size * 0.015f, size * 0.025f, paint)
        paint.color = AliceAvatarPalette.ACCENT
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = size * 0.018f
        canvas.drawArc(cx - size * 0.09f, cy + size * 0.035f,
            cx + size * 0.09f, cy + size * 0.14f,
            if (emotion == AliceEmotion.HAPPY) 5f else 25f,
            if (emotion == AliceEmotion.HAPPY) 170f else 130f, false, paint)
        paint.style = Paint.Style.FILL
    }
}
