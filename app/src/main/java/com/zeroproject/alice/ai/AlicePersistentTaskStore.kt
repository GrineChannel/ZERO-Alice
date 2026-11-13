package com.zeroproject.alice.ai

import android.content.ContentValues
import android.database.Cursor

class AlicePersistentTaskStore(private val database: AliceTaskDatabase) {
    fun save(task: AliceTask) {
        val values = ContentValues().apply {
            put("id", task.id)
            put("request", task.request)
            put("capability", task.capability.name)
            put("mode", task.mode.name)
            put("status", task.status.name)
            put("created_at", task.createdAt)
        }
        database.writableDatabase.insertWithOnConflict(
            "agent_tasks", null, values,
            android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    fun find(id: String): AliceTask? =
        database.readableDatabase.query(
            "agent_tasks", null, "id = ?", arrayOf(id),
            null, null, null
        ).use { cursor ->
            if (cursor.moveToFirst()) read(cursor) else null
        }

    fun list(limit: Int = 100): List<AliceTask> {
        require(limit in 1..1000)
        return database.readableDatabase.query(
            "agent_tasks", null, null, null, null, null,
            "created_at DESC", limit.toString()
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(read(cursor))
            }
        }
    }

    fun updateStatus(id: String, status: AliceTaskStatus): Boolean {
        val values = ContentValues().apply { put("status", status.name) }
        return database.writableDatabase.update(
            "agent_tasks", values, "id = ?", arrayOf(id)
        ) > 0
    }

    private fun read(cursor: Cursor): AliceTask = AliceTask(
        id = cursor.getString(cursor.getColumnIndexOrThrow("id")),
        request = cursor.getString(cursor.getColumnIndexOrThrow("request")),
        capability = AliceCapability.valueOf(
            cursor.getString(cursor.getColumnIndexOrThrow("capability"))
        ),
        mode = AliceExecutionMode.valueOf(
            cursor.getString(cursor.getColumnIndexOrThrow("mode"))
        ),
        status = AliceTaskStatus.valueOf(
            cursor.getString(cursor.getColumnIndexOrThrow("status"))
        ),
        createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at"))
    )
}
