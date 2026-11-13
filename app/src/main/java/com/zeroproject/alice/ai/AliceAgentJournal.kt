package com.zeroproject.alice.ai

import android.content.ContentValues

data class AliceAgentEvent(
    val taskId: String,
    val kind: String,
    val details: String,
    val timestamp: Long
)

class AliceAgentJournal(private val database: AliceTaskDatabase) {
    fun record(taskId: String, kind: String, details: String) {
        require(taskId.isNotBlank() && kind.isNotBlank())
        val values = ContentValues().apply {
            put("task_id", taskId)
            put("kind", kind)
            put("details", details.take(4000))
            put("created_at", System.currentTimeMillis())
        }
        database.writableDatabase.insertOrThrow("agent_events", null, values)
    }

    fun recent(taskId: String, limit: Int = 100): List<AliceAgentEvent> {
        require(limit in 1..1000)
        return database.readableDatabase.query(
            "agent_events", null, "task_id = ?", arrayOf(taskId),
            null, null, "id DESC", limit.toString()
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(AliceAgentEvent(
                        taskId = cursor.getString(cursor.getColumnIndexOrThrow("task_id")),
                        kind = cursor.getString(cursor.getColumnIndexOrThrow("kind")),
                        details = cursor.getString(cursor.getColumnIndexOrThrow("details")),
                        timestamp = cursor.getLong(cursor.getColumnIndexOrThrow("created_at"))
                    ))
                }
            }
        }
    }
}
