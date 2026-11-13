package com.zeroproject.alice.ui
import android.content.Context
import android.widget.LinearLayout
object AliceSectionHeader {
    fun add(container: LinearLayout, title: String, subtitle: String) {
        container.addView(AliceTextFactory.title(container.context, title))
        container.addView(AliceTextFactory.subtitle(container.context, subtitle))
    }
}
