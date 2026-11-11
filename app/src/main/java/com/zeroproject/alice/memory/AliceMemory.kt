
package com.zeroproject.alice.memory

import android.content.Context
import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class AliceMessage(
    val id: Long,
    val role: String,
    val content: String,
    val timestamp: Long
)

data class AliceFact(
    val id: Long,
    val key: String,
    val value: String,
    val timestamp: Long
)

class AliceMemory(context: Context) {

    private val helper = MemoryDatabase(context.applicationContext)

    // Сохранение сообщения в историю.

    fun saveMessage(role: String, content: String): Long {
        if (role.isBlank() || content.isBlank()) return -1

        val values = ContentValues().apply {
            put("role", role)
            put("content", content)
            put("timestamp", System.currentTimeMillis())
        }

        return helper.writableDatabase.insert(
            "messages",
            null,
            values
        )
    }

    // Возвращаем последние сообщения в хронологическом порядке.
    // Ранее запрос id ASC с LIMIT возвращал самые старые записи,
    // поэтому новые сообщения пропадали из контекста после 5000 записей.
    fun getMessages(limit: Int = 500): List<AliceMessage> {
        val result = mutableListOf<AliceMessage>()

        val cursor = helper.readableDatabase.query(
            "messages",
            null,
            null,
            null,
            null,
            null,
            "id DESC",
            limit.coerceIn(1, 5000).toString()
        )

        cursor.use {
            while (it.moveToNext()) {
                result.add(
                    AliceMessage(
                        id = it.getLong(it.getColumnIndexOrThrow("id")),
                        role = it.getString(it.getColumnIndexOrThrow("role")),
                        content = it.getString(it.getColumnIndexOrThrow("content")),
                        timestamp = it.getLong(it.getColumnIndexOrThrow("timestamp"))
                    )
                )
            }
        }

        result.reverse()
        return result
    }

    // Очистка истории переписки.

    fun clearHistory() {
        helper.writableDatabase.delete(
            "messages",
            null,
            null
        )
    }

    // Сохранение или обновление факта.

    fun saveFact(key: String, value: String): Long {
        if (key.isBlank() || value.isBlank()) return -1

        val values = ContentValues().apply {
            put("fact_key", key)
            put("fact_value", value)
            put("timestamp", System.currentTimeMillis())
        }

        return helper.writableDatabase.insertWithOnConflict(
            "facts",
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    // Получение всех сохранённых фактов.

    fun getFacts(): List<AliceFact> {
        val result = mutableListOf<AliceFact>()

        val cursor = helper.readableDatabase.query(
            "facts",
            null,
            null,
            null,
            null,
            null,
            "id ASC"
        )

        cursor.use {
            while (it.moveToNext()) {
                result.add(
                    AliceFact(
                        id = it.getLong(
                            it.getColumnIndexOrThrow("id")
                        ),
                        key = it.getString(
                            it.getColumnIndexOrThrow("fact_key")
                        ),
                        value = it.getString(
                            it.getColumnIndexOrThrow("fact_value")
                        ),
                        timestamp = it.getLong(
                            it.getColumnIndexOrThrow("timestamp")
                        )
                    )
                )
            }
        }

        return result
    }

    // Удаление одного факта.

    fun deleteFact(id: Long) {
        helper.writableDatabase.delete(
            "facts",
            "id = ?",
            arrayOf(id.toString())
        )
    }

    // Полная очистка долговременной памяти.

    fun clearFacts() {
        helper.writableDatabase.delete(
            "facts",
            null,
            null
        )
    }

    // Закрытие базы данных.

    fun close() {
        helper.close()
    }

    // SQLite база данных.

    private class MemoryDatabase(context: Context) :
        SQLiteOpenHelper(
            context,
            "alice_memory.db",
            null,
            1
        ) {

        override fun onCreate(db: SQLiteDatabase) {

            db.execSQL(
                """
                CREATE TABLE messages (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    role TEXT NOT NULL,
                    content TEXT NOT NULL,
                    timestamp INTEGER NOT NULL
                )
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TABLE facts (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    fact_key TEXT NOT NULL UNIQUE,
                    fact_value TEXT NOT NULL,
                    timestamp INTEGER NOT NULL
                )
                """.trimIndent()
            )
        }

        override fun onUpgrade(
            db: SQLiteDatabase,
            oldVersion: Int,
            newVersion: Int
        ) {
            // Миграции добавим при изменении структуры базы.
        }
    }
}
