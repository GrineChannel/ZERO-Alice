
package com.zeroproject.alice.screens

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

import com.zeroproject.alice.engineer.minecraft.MinecraftProject

class AliceEngineerScreen(
    private val context: Context,
    private val onBackClick: () -> Unit,
    private val onCreateProject: (String, String) -> Unit,
    private val onDevelop: (String, String) -> Unit
) {

    private val backgroundColor = Color.rgb(13, 16, 25)
    private val panelColor = Color.rgb(24, 29, 43)
    private val accentColor = Color.rgb(80, 160, 255)
    private val textColor = Color.WHITE
    private val mutedColor = Color.rgb(165, 175, 195)

    private val root = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setBackgroundColor(backgroundColor)
        setPadding(dp(18), dp(14), dp(18), dp(14))
    }

    private val projectContainer = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
    }

    private val projectNameInput = EditText(context).apply {
        hint = "Название проекта"
        setHintTextColor(mutedColor)
        setTextColor(textColor)
        textSize = 15f
        setSingleLine(true)
        setPadding(dp(12), dp(10), dp(12), dp(10))
        background = roundedBackground(panelColor, 10)
    }

    private val projectDescriptionInput = EditText(context).apply {
        hint = "Описание проекта"
        setHintTextColor(mutedColor)
        setTextColor(textColor)
        textSize = 15f
        minLines = 2
        gravity = Gravity.TOP
        setPadding(dp(12), dp(10), dp(12), dp(10))
        background = roundedBackground(panelColor, 10)
    }

    private val goalInput = EditText(context).apply {
        hint = "Опиши, что должна разработать Элис..."
        setHintTextColor(mutedColor)
        setTextColor(textColor)
        textSize = 15f
        minLines = 4
        gravity = Gravity.TOP
        setPadding(dp(12), dp(10), dp(12), dp(10))
        background = roundedBackground(panelColor, 10)
    }

    private val statusText = TextView(context).apply {
        text = "Элис готова к работе"
        textSize = 14f
        setTextColor(mutedColor)
        setPadding(0, dp(12), 0, dp(12))
    }

    private val selectedProjectText = TextView(context).apply {
        text = "Проект не выбран"
        textSize = 14f
        setTextColor(accentColor)
        setPadding(0, dp(8), 0, dp(8))
    }

    private val developButton = createButton("НАЧАТЬ РАЗРАБОТКУ")

    private var projects: List<MinecraftProject> = emptyList()
    private var selectedProjectId: String? = null
    private var busy = false

    fun createView(): View {
        root.removeAllViews()

        val header = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val backButton = createButton("←")
        backButton.textSize = 20f

        header.addView(
            backButton,
            LinearLayout.LayoutParams(dp(52), dp(48))
        )

        backButton.setOnClickListener {
            onBackClick()
        }

        val title = TextView(context).apply {
            text = "ИНЖЕНЕРНАЯ ЛАБОРАТОРИЯ"
            textSize = 17f
            setTextColor(textColor)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), 0, 0, 0)
        }

        header.addView(
            title,
            LinearLayout.LayoutParams(
                0,
                dp(48),
                1f
            )
        )

        root.addView(header)

        addSectionTitle("MINECRAFT BEDROCK")

        addDescription(
            "Создание, программирование и проверка игровых дополнений."
        )

        addSectionTitle("МОИ ПРОЕКТЫ")

        val projectScroll = ScrollView(context).apply {
            isFillViewport = false
        }

        projectScroll.addView(projectContainer)

        root.addView(
            projectScroll,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(150)
            )
        )

        addSectionTitle("НОВЫЙ ПРОЕКТ")

        root.addView(
            projectNameInput,
            marginParams(bottom = 8)
        )

        root.addView(
            projectDescriptionInput,
            marginParams(bottom = 8)
        )

        val createButton = createButton("СОЗДАТЬ ПРОЕКТ")

        root.addView(
            createButton,
            marginParams(bottom = 8)
        )

        createButton.setOnClickListener {
            val name = projectNameInput.text.toString().trim()

            val description =
                projectDescriptionInput.text.toString().trim()

            if (name.isBlank()) {
                setStatus("Укажи название проекта")
                return@setOnClickListener
            }

            if (busy) return@setOnClickListener

            onCreateProject(name, description)
        }

        addSectionTitle("ЗАДАНИЕ ДЛЯ ЭЛИС")

        root.addView(selectedProjectText)

        root.addView(
            goalInput,
            marginParams(bottom = 10)
        )

        root.addView(
            developButton,
            marginParams(bottom = 8)
        )

        developButton.setOnClickListener {
            val projectId = selectedProjectId
            val goal = goalInput.text.toString().trim()

            if (projectId == null) {
                setStatus("Сначала выбери проект")
                return@setOnClickListener
            }

            if (goal.isBlank()) {
                setStatus("Опиши задачу для Элис")
                return@setOnClickListener
            }

            if (busy) return@setOnClickListener

            onDevelop(projectId, goal)
        }

        addSectionTitle("СОСТОЯНИЕ")

        root.addView(statusText)

        return root
    }

    fun updateProjects(newProjects: List<MinecraftProject>) {
        projects = newProjects

        if (projects.none { it.id == selectedProjectId }) {
            selectedProjectId = null
        }

        projectContainer.removeAllViews()

        if (projects.isEmpty()) {
            val emptyText = TextView(context).apply {
                text = "Пока нет проектов. Создай первый!"
                textSize = 14f
                setTextColor(mutedColor)
                setPadding(dp(8), dp(12), dp(8), dp(12))
            }

            projectContainer.addView(emptyText)
        }

        projects.forEach { project ->
            val item = TextView(context).apply {
                text = if (project.id == selectedProjectId) {
                    "✓  ${project.name}"
                } else {
                    project.name
                }

                textSize = 15f

                setTextColor(
                    if (project.id == selectedProjectId) {
                        accentColor
                    } else {
                        textColor
                    }
                )

                setPadding(dp(12), dp(14), dp(12), dp(14))

                background = roundedBackground(panelColor, 10)

                isClickable = true
                isFocusable = true
            }

            item.setOnClickListener {
                selectedProjectId = project.id

                selectedProjectText.text =
                    "Выбран проект: ${project.name}"

                updateProjects(projects)
            }

            projectContainer.addView(
                item,
                marginParams(bottom = 6)
            )
        }

        val selected = projects.firstOrNull {
            it.id == selectedProjectId
        }

        selectedProjectText.text = selected?.let {
            "Выбран проект: ${it.name}"
        } ?: "Проект не выбран"
    }

    fun setStatus(message: String) {
        statusText.text = message
    }

    fun setBusy(value: Boolean) {
        busy = value
        developButton.isEnabled = !value
        developButton.alpha = if (value) 0.5f else 1f
    }

    fun clearGoal() {
        goalInput.text.clear()
    }

    fun showResult(message: String) {
        setBusy(false)
        setStatus(message)
    }

    private fun addSectionTitle(value: String) {
        val title = TextView(context).apply {
            text = value
            textSize = 13f
            setTextColor(accentColor)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, dp(18), 0, dp(8))
        }

        root.addView(title)
    }

    private fun addDescription(value: String) {
        val description = TextView(context).apply {
            text = value
            textSize = 13f
            setTextColor(mutedColor)
            setPadding(0, 0, 0, dp(8))
        }

        root.addView(description)
    }

    private fun createButton(value: String): Button {
        return Button(context).apply {
            text = value
            textSize = 12f
            setTextColor(Color.WHITE)
            isAllCaps = false

            background = roundedBackground(
                Color.rgb(37, 91, 155),
                10
            )
        }
    }

    private fun roundedBackground(
        color: Int,
        radius: Int
    ): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
        }
    }

    private fun marginParams(
        bottom: Int = 0
    ): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            if (bottom > 0) {
                bottomMargin = dp(bottom)
            }
        }
    }

    private fun dp(value: Int): Int {
        return (
            value * context.resources.displayMetrics.density
        ).toInt()
    }
}
