
package com.zeroproject.alice.engineer

import com.zeroproject.alice.AliceLocalEngine
import org.json.JSONArray
import org.json.JSONObject

data class EngineerPlanStep(
    val id: Int,
    val title: String,
    val description: String,
    val expectedFiles: List<String>
)

data class EngineerPlan(
    val summary: String,
    val steps: List<EngineerPlanStep>
)

class AliceEngineerPlanner(
    private val engine: AliceLocalEngine
) {

    fun createPlan(
        goal: String,
        minecraftVersion: String = "1.21.43",
        scriptApiVersion: String = "1.16.0-beta",
        onResult: (EngineerPlan) -> Unit,
        onError: (String) -> Unit
    ) {

        if (goal.isBlank()) {
            onError("Описание инженерной задачи пустое")
            return
        }

        val prompt = buildPrompt(
            goal = goal,
            minecraftVersion = minecraftVersion,
            scriptApiVersion = scriptApiVersion
        )

        engine.analyzeTask(
            prompt = prompt,
            onResult = { response ->

                try {
                    val plan = parsePlan(response)
                    onResult(plan)

                } catch (error: Throwable) {
                    onError(
                        error.message ?: "Не удалось обработать план"
                    )
                }
            },
            onError = onError
        )
    }

    private fun buildPrompt(
        goal: String,
        minecraftVersion: String,
        scriptApiVersion: String
    ): String {

        return """
            Создай технический план разработки Minecraft Bedrock Add-on.

            ЗАДАЧА:
            $goal

            ВЕРСИЯ MINECRAFT:
            $minecraftVersion

            ВЕРСИЯ SCRIPT API:
            $scriptApiVersion

            ТРЕБОВАНИЯ:

            1. Раздели работу на последовательные этапы.
            2. Учитывай Behavior Pack и Resource Pack,
               если они необходимы.
            3. Указывай реальные относительные пути файлов.
            4. Не создавай лишние файлы.
            5. Учитывай совместимость API.
            6. Не придумывай несуществующие возможности.
            7. План должен быть пригоден для последующей
               автоматической генерации файлов.
            8. Максимум 12 этапов.
            9. Каждый этап должен иметь конкретный результат.

            ВЕРНИ ТОЛЬКО JSON:

            {
              "summary": "Краткое описание решения",
              "steps": [
                {
                  "id": 1,
                  "title": "Название этапа",
                  "description": "Что необходимо сделать",
                  "expectedFiles": [
                    "behavior_pack/manifest.json"
                  ]
                }
              ]
            }

            Не используй Markdown.
            Не добавляй текст до или после JSON.
        """.trimIndent()
    }

    private fun parsePlan(response: String): EngineerPlan {

        val jsonText = extractJson(response)

        val json = JSONObject(jsonText)

        val summary = json.getString("summary").trim()

        require(summary.isNotBlank()) {
            "План не содержит описания"
        }

        val jsonSteps = json.getJSONArray("steps")

        require(jsonSteps.length() in 1..12) {
            "План должен содержать от 1 до 12 этапов"
        }

        val steps = mutableListOf<EngineerPlanStep>()

        val usedIds = mutableSetOf<Int>()

        for (i in 0 until jsonSteps.length()) {

            val item = jsonSteps.getJSONObject(i)

            val id = item.getInt("id")
            val title = item.getString("title").trim()
            val description = item.getString("description").trim()

            require(id > 0 && usedIds.add(id)) {
                "Некорректный или повторяющийся номер этапа"
            }

            require(title.isNotBlank()) {
                "Этап $id не имеет названия"
            }

            require(description.isNotBlank()) {
                "Этап $id не имеет описания"
            }

            val filesJson = item.optJSONArray("expectedFiles")
                ?: JSONArray()

            require(filesJson.length() <= 30) {
                "Слишком много файлов в этапе $id"
            }

            val files = mutableListOf<String>()

            for (fileIndex in 0 until filesJson.length()) {

                val path = filesJson.getString(fileIndex).trim()

                require(isSafeRelativePath(path)) {
                    "Недопустимый путь файла: $path"
                }

                files.add(path)
            }

            steps.add(
                EngineerPlanStep(
                    id = id,
                    title = title,
                    description = description,
                    expectedFiles = files
                )
            )
        }

        return EngineerPlan(
            summary = summary,
            steps = steps
        )
    }

    private fun extractJson(response: String): String {

        val cleaned = response
            .replace("```json", "")
            .replace("```", "")
            .trim()

        val start = cleaned.indexOf('{')
        val end = cleaned.lastIndexOf('}')

        require(start >= 0 && end > start) {
            "Gemma не вернула корректный JSON"
        }

        return cleaned.substring(start, end + 1)
    }

    private fun isSafeRelativePath(path: String): Boolean {

        if (path.isBlank()) return false

        if (path.length > 200) return false

        if (path.startsWith("/") || path.startsWith("\\")) {
            return false
        }

        if (path.contains('\\')) return false

        if (path.contains(':')) return false

        val segments = path.split('/')

        if (segments.any {
                it.isBlank() || it == "." || it == ".."
            }) {
            return false
        }

        return true
    }
}
