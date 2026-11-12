package com.zeroproject.alice.tasks

import android.content.Context
import org.json.JSONObject
import java.io.File

/**
 * Bounded, app-private task execution journal.
 * Stores only execution metadata and short messages, never file contents.
 */
class AliceTaskLog(context: Context) {
    private val file = File(context.filesDir, "alice_task_log.jsonl")
    private val lock = Any()

    data class Record(
        val taskId: String,
        val stepIndex: Int,
        val success: Boolean,
        val message: String,
        val timestamp: Long
    )

    fun append(taskId: String, stepIndex: Int, success: Boolean, message: String) {
        synchronized(lock) {
            val entry = JSONObject()
                .put("taskId", taskId)
                .put("stepIndex", stepIndex)
                .put("success", success)
                .put("message", message.take(500))
                .put("timestamp", System.currentTimeMillis())
            file.appendText(entry.toString() + "\n", Charsets.UTF_8)
            if (file.length() > 256 * 1024) {
                val recent = file.readLines(Charsets.UTF_8).takeLast(300)
                val temp = File(file.parentFile, "${file.name}.tmp")
                temp.writeText(recent.joinToString("\n", postfix = "\n"), Charsets.UTF_8)
                if (!temp.renameTo(file)) temp.delete()
            }
        }
    }

    fun recent(taskId: String, limit: Int = 20): List<Record> = synchronized(lock) {
        if (!file.exists()) return@synchronized emptyList()
        file.readLines(Charsets.UTF_8).takeLast(300).mapNotNull { line ->
            try {
                val entry = JSONObject(line)
                if (entry.getString("taskId") != taskId) null
                else Record(
                    entry.getString("taskId"),
                    entry.getInt("stepIndex"),
                    entry.getBoolean("success"),
                    entry.getString("message"),
                    entry.getLong("timestamp")
                )
            } catch (_: Exception) {
                null
            }
        }.takeLast(limit.coerceIn(1, 50))
    }
}
