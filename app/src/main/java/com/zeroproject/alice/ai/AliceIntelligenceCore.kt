package com.zeroproject.alice.ai

import java.util.UUID

enum class AliceCapability {
    CONVERSATION, MUSIC, PROGRAMMING, ASSISTANT, CREATIVE, RESEARCH, GENERAL
}

enum class AliceExecutionMode { LOCAL, CLOUD, HYBRID }

enum class AliceTaskStatus {
    CREATED, PLANNED, RUNNING, COMPLETED, FAILED, CANCELLED
}

data class AliceTask(
    val id: String = UUID.randomUUID().toString(),
    val request: String,
    val capability: AliceCapability,
    val mode: AliceExecutionMode,
    val status: AliceTaskStatus = AliceTaskStatus.CREATED,
    val createdAt: Long = System.currentTimeMillis()
)

class AliceIntelligenceCore {
    fun createTask(
        request: String,
        cloudAvailable: Boolean = false,
        cloudAllowed: Boolean = false
    ): AliceTask {
        require(request.isNotBlank()) { "Task request cannot be empty" }
        val capability = detectCapability(request)
        return AliceTask(
            request = request.trim(),
            capability = capability,
            mode = selectMode(capability, cloudAvailable, cloudAllowed)
        )
    }

    fun detectCapability(request: String): AliceCapability {
        val text = request.lowercase()
        return when {
            containsAny(text, "музык", "песн", "мелоди", "бит", "аранжиров", "music", "song", "melody") -> AliceCapability.MUSIC
            containsAny(text, "код", "программ", "github", "minecraft", "мод", "ошибк", "сборк", "code", "debug") -> AliceCapability.PROGRAMMING
            containsAny(text, "напомни", "расписани", "запланируй", "будильник", "remind", "schedule") -> AliceCapability.ASSISTANT
            containsAny(text, "нарисуй", "истори", "книг", "сценари", "draw", "story") -> AliceCapability.CREATIVE
            containsAny(text, "найди", "исследуй", "сравни", "проверь информацию", "research", "search") -> AliceCapability.RESEARCH
            containsAny(text, "привет", "поговорим", "как дела", "мне грустно", "hello", "chat") -> AliceCapability.CONVERSATION
            else -> AliceCapability.GENERAL
        }
    }

    fun selectMode(
        capability: AliceCapability,
        cloudAvailable: Boolean,
        cloudAllowed: Boolean
    ): AliceExecutionMode {
        if (!cloudAvailable || !cloudAllowed) return AliceExecutionMode.LOCAL
        return when (capability) {
            AliceCapability.PROGRAMMING, AliceCapability.RESEARCH,
            AliceCapability.MUSIC, AliceCapability.CREATIVE -> AliceExecutionMode.HYBRID
            else -> AliceExecutionMode.LOCAL
        }
    }

    fun describeTask(task: AliceTask): String = buildString {
        appendLine("ZERO Intelligence Core")
        appendLine("Task ID: ${task.id}")
        appendLine("Capability: ${task.capability}")
        appendLine("Execution: ${task.mode}")
        appendLine("Status: ${task.status}")
        appendLine("Request: ${task.request}")
    }

    private fun containsAny(text: String, vararg keywords: String): Boolean =
        keywords.any { text.contains(it) }
}
