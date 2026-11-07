
package com.zeroproject.alice.memory

import org.json.JSONArray
import org.json.JSONObject

data class ExtractedMemory(
    val key: String,
    val value: String
)

class AliceMemoryExtractor {

    /**
     * Создаёт изолированный запрос для Gemma.
     */
    fun buildPrompt(dialogue: String): String {

        return """
            Ты — модуль долговременной памяти Элис.

            Твоя задача — анализировать диалог и выделять
            только действительно полезную информацию
            для будущих разговоров.

            ПРАВИЛА:

            1. Анализируй сообщения пользователя и Элис.
            2. Сохраняй долгосрочные предпочтения,
               интересы, цели, планы и проекты.
            3. Сохраняй важные технические решения,
               договорённости и результаты совместной работы.
            4. Не сохраняй случайные фразы и обычную болтовню.
            5. Не превращай предположения Элис в факты.
            6. Сохраняй утверждения Элис только тогда,
               когда они явно подтверждены пользователем.
            7. Не сохраняй пароли, токены, ключи доступа,
               платёжные реквизиты и другие секреты.
            8. Не дублируй одну и ту же информацию.
            9. Если информации для памяти нет,
               верни пустой массив.
            10. Не придумывай отсутствующие сведения.

            ФОРМАТ ОТВЕТА:

            Верни только JSON без Markdown и пояснений:

            {
              "memories": [
                {
                  "key": "название категории",
                  "value": "сохранённая информация"
                }
              ]
            }

            ПРИМЕР:

            {
              "memories": [
                {
                  "key": "текущий проект",
                  "value": "Пользователь разрабатывает ZERO: Alice"
                },
                {
                  "key": "техническое решение",
                  "value": "Для приложения используется Kotlin и Android SDK"
                }
              ]
            }

            ЕСЛИ СОХРАНЯТЬ НЕЧЕГО:

            {
              "memories": []
            }

            ДИАЛОГ ДЛЯ АНАЛИЗА:

            $dialogue
        """.trimIndent()
    }

    /**
     * Разбирает JSON, полученный от Gemma.
     */
    fun parseResponse(response: String): List<ExtractedMemory> {

        return try {

            val cleaned = response
                .trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            val start = cleaned.indexOf('{')
            val end = cleaned.lastIndexOf('}')

            if (start < 0 || end <= start) {
                return emptyList()
            }

            val jsonText = cleaned.substring(
                start,
                end + 1
            )

            val json = JSONObject(jsonText)

            val memories = json.optJSONArray("memories")
                ?: JSONArray()

            val result = mutableListOf<ExtractedMemory>()

            for (i in 0 until memories.length()) {

                val item = memories.optJSONObject(i)
                    ?: continue

                val key = item.optString("key")
                    .trim()

                val value = item.optString("value")
                    .trim()

                if (key.isBlank() || value.isBlank()) {
                    continue
                }

                if (key.length > 100 || value.length > 500) {
                    continue
                }

                result.add(
                    ExtractedMemory(
                        key = key,
                        value = value
                    )
                )
            }

            result.distinctBy {
                "${it.key.lowercase()}:${it.value.lowercase()}"
            }

        } catch (_: Throwable) {
            emptyList()
        }
    }
}
