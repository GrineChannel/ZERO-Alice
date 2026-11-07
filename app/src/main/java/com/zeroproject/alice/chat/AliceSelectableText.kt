
package com.zeroproject.alice.chat

import android.content.Context
import android.graphics.Color
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.TextView

object AliceSelectableText {

    fun create(
        context: Context,
        text: String,
        textColor: Int = Color.WHITE,
        textSize: Float = 16f
    ): TextView {

        return TextView(context).apply {

            this.text = text

            setTextColor(textColor)

            setTextSize(
                TypedValue.COMPLEX_UNIT_SP,
                textSize
            )

            gravity = Gravity.START

            // Разрешаем выделение отдельных фрагментов
            setTextIsSelectable(true)

            // Системное меню Android:
            // Копировать, Выделить всё и другие действия
            isLongClickable = true

            // Перенос текста
            setLineSpacing(
                0f,
                1.15f
            )

            // Не отключаем стандартные маркеры выделения
            isFocusable = true
            isFocusableInTouchMode = true

            // Текст не редактируется
            isCursorVisible = false
        }
    }
}
