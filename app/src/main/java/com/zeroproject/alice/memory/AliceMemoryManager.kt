
package com.zeroproject.alice.memory

import com.zeroproject.alice.AliceLocalEngine
import java.util.ArrayDeque

class AliceMemoryManager(
    private val memory: AliceMemory,
    private val engine: AliceLocalEngine
) {
    private data class PendingAnalysis(
        val dialogue: String,
        val generation: Long,
        val onComplete: (Int) -> Unit,
        val onError: (String) -> Unit
    )

    private val extractor = AliceMemoryExtractor()
    private val queue = ArrayDeque<PendingAnalysis>()
    private val queueLock = Any()

    private var analysisRunning = false
    private var generation = 0L

    // Invalidate queued and in-flight extraction after memory commands.
    // Results from earlier generations must never write back into memory.
    fun invalidatePendingAnalyses() {
        synchronized(queueLock) {
            generation++
            queue.clear()
        }
    }

    // Serialize command execution with background fact writes. A command
    // cannot clear memory between a generation check and a database write.
    fun executeMemoryCommand(action: () -> String?): String? {
        return synchronized(queueLock) {
            val response = action()
            if (response != null) {
                generation++
                queue.clear()
            }
            response
        }
    }

    fun analyzeDialogue(
        dialogue: String,
        onComplete: (Int) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        if (dialogue.isBlank()) {
            onComplete(0)
            return
        }

        synchronized(queueLock) {
            queue.addLast(
                PendingAnalysis(dialogue, generation, onComplete, onError)
            )
        }

        processNext()
    }

    private fun processNext() {
        val task = synchronized(queueLock) {
            if (analysisRunning) return

            val next = queue.pollFirst() ?: return
            analysisRunning = true
            next
        }

        try {
            val prompt = extractor.buildPrompt(task.dialogue)

            engine.analyzeMemory(
                prompt = prompt,
                onResult = { response ->
                    try {
                        val extracted = extractor.parseResponse(response)
                        var savedCount = 0

                        for (fact in extracted) {
                            if (synchronized(queueLock) { task.generation != generation }) {
                                break
                            }
                            if (
                                containsSensitiveInformation(
                                    fact.key + " " + fact.value
                                )
                            ) {
                                continue
                            }

                            if (saveOrUpdateFact(fact, task.generation)) {
                                savedCount++
                            }
                        }

                        task.onComplete(savedCount)

                    } catch (error: Throwable) {
                        task.onError(
                            error.message ?: "Ошибка сохранения памяти"
                        )
                    } finally {
                        finishCurrent()
                    }
                },
                onError = { error ->
                    try {
                        task.onError(error)
                    } finally {
                        finishCurrent()
                    }
                }
            )
        } catch (error: Throwable) {
            try {
                task.onError(
                    error.message ?: "Ошибка анализа памяти"
                )
            } finally {
                finishCurrent()
            }
        }
    }

    private fun finishCurrent() {
        synchronized(queueLock) {
            analysisRunning = false
        }

        processNext()
    }

    // Check exclusions immediately before writing, including results from
    // analysis tasks queued before the user requested forgetting.
    private fun wasForgotten(key: String, value: String): Boolean {
        val terms = memory.getForgottenTerms()
        return terms.any { term ->
            if (term.startsWith("key:")) {
                key.equals(term.removePrefix("key:"), ignoreCase = true)
            } else {
                value.contains(term, ignoreCase = true) ||
                    key.contains(term, ignoreCase = true)
            }
        }
    }

    private fun saveOrUpdateFact(fact: ExtractedMemory, taskGeneration: Long): Boolean = synchronized(queueLock) {
        if (taskGeneration != generation) return false
        val key = fact.key.trim()
        val value = fact.value.trim()

        if (key.isBlank() || value.isBlank()) return false
        if (wasForgotten(key, value)) return false
        if (taskGeneration != generation) return false
        if (key.length > 100 || value.length > 500) return false

        val existingFacts = memory.getFacts()

        val existing = existingFacts.firstOrNull {
            it.key.equals(key, ignoreCase = true)
        }

        if (existing != null) {
            if (existing.value.equals(value, ignoreCase = true)) {
                return false
            }

            memory.saveFact(key, value)
            return true
        }

        if (existingFacts.size >= 500) return false

        memory.saveFact(key, value)
        return true
    }

    private fun containsSensitiveInformation(text: String): Boolean {
        val patterns = listOf(
            Regex("""(?i)\bpassword\b"""),
            Regex("""(?i)\bпароль\b"""),
            Regex("""(?i)\btoken\b"""),
            Regex("""(?i)\bapi[_ -]?key\b"""),
            Regex("""(?i)\bsecret\b"""),
            Regex("""(?i)\bprivate[_ -]?key\b"""),
            Regex("""(?i)\bключ доступа\b"""),
            Regex("""(?i)\bномер карты\b"""),
            Regex("""(?i)\bcvv\b""")
        )

        return patterns.any { it.containsMatchIn(text) }
    }

    fun getMemories(): List<AliceFact> {
        return memory.getFacts()
    }

    fun forget(id: Long) {
        memory.deleteFact(id)
    }

    fun clear() {
        memory.clearFacts()
    }

    fun isAnalyzing(): Boolean {
        return synchronized(queueLock) {
            analysisRunning || queue.isNotEmpty()
        }
    }
}
