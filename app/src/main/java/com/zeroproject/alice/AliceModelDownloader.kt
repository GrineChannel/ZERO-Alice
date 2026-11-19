
package com.zeroproject.alice

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.StatFs
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class AliceModelDownloader(context: Context) {

    companion object {
        const val MODEL_NAME = "gemma-4-E2B-it.litertlm"

        private const val MODEL_URL =
            "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it.litertlm"

        private const val EXPECTED_SHA256 =
            "181938105e0eefd105961417e8da75903eacda102c4fce9ce90f50b97139a63c"

        private const val BUFFER_SIZE = 1024 * 1024
    }

    interface Listener {
        fun onProgress(downloaded: Long, total: Long, percent: Int)
        fun onComplete(file: File)
        fun onError(message: String)
        fun onCancelled()
    }

    private val appContext = context.applicationContext
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    private val modelFile =
        File(appContext.filesDir, MODEL_NAME)

    private val partialFile =
        File(appContext.filesDir, "$MODEL_NAME.part")

    private val downloadRunning = AtomicBoolean(false)

    @Volatile
    private var cancelled = false

    @Volatile
    private var connection: HttpURLConnection? = null

    fun getModelFile(): File = modelFile

    fun isModelDownloaded(): Boolean {
        return modelFile.isFile && modelFile.length() > 0
    }

    fun start(listener: Listener) {
        if (executor.isShutdown) {
            listenerError(listener, "Загрузчик закрыт")
            return
        }

        if (!downloadRunning.compareAndSet(false, true)) {
            listenerError(listener, "Загрузка модели уже выполняется")
            return
        }
        cancelled = false
        try {
            executor.execute {
                try {
                    download(listener)
                } finally {
                    downloadRunning.set(false)
                }
            }
        } catch (error: RuntimeException) {
            downloadRunning.set(false)
            listenerError(listener, error.message ?: "Не удалось начать загрузку")
        }
    }

    fun cancel() {
        cancelled = true
        connection?.disconnect()
    }

    fun close() {
        cancel()
        executor.shutdown()
    }

    private fun download(listener: Listener) {
        try {
            recoverInterruptedReplacement()
            if (isModelDownloaded()) {
                if (verifyModel(modelFile)) {
                    listenerComplete(listener, modelFile)
                    return
                }
                // Keep the existing file until a new download passes SHA-256.
                // Replacement happens only after verification below.
            }

            val existingBytes =
                if (partialFile.exists()) partialFile.length()
                else 0L

            val conn =
                (URL(MODEL_URL).openConnection() as HttpURLConnection)

            connection = conn

            conn.connectTimeout = 30000
            conn.readTimeout = 60000
            conn.instanceFollowRedirects = true
            conn.requestMethod = "GET"
            conn.setRequestProperty(
                "User-Agent",
                "ZERO-Alice-Android"
            )

            if (existingBytes > 0) {
                conn.setRequestProperty(
                    "Range",
                    "bytes=$existingBytes-"
                )
            }

            conn.connect()

            if (cancelled) {
                listenerCancelled(listener)
                return
            }

            val append: Boolean
            val totalBytes: Long

            when (conn.responseCode) {
                HttpURLConnection.HTTP_PARTIAL -> {
                    // A resumed response must begin exactly at our saved byte offset.
                    val range = conn.getHeaderField("Content-Range")
                        ?: throw Exception("Сервер не указал Content-Range")
                    val start = range.substringAfter("bytes ", "")
                        .substringBefore("-").toLongOrNull()
                    if (existingBytes <= 0L || start != existingBytes) {
                        throw Exception("Сервер вернул неверный диапазон загрузки")
                    }
                    append = true

                    totalBytes =
                        conn.getHeaderField("Content-Range")
                            ?.substringAfter("/")
                            ?.toLongOrNull()
                            ?: throw Exception(
                                "Не удалось определить размер файла"
                            )
                }

                HttpURLConnection.HTTP_OK -> {
                    append = false
                    totalBytes = conn.contentLengthLong

                    // Keep the resumable partial file until free-space checks pass.
                    // FileOutputStream(partialFile, false) will truncate it only
                    // when the replacement download is ready to begin.
                }

                else -> throw Exception(
                    "Ошибка сервера: HTTP ${conn.responseCode}"
                )
            }

            if (totalBytes <= 0) {
                throw Exception("Некорректный размер модели")
            }

            val currentBytes =
                if (append) existingBytes else 0L

            if (currentBytes > totalBytes) {
                throw Exception("Некорректный размер частичного файла")
            }

            val requiredBytes = totalBytes - currentBytes

            val availableBytes =
                StatFs(appContext.filesDir.path).availableBytes

            if (availableBytes < requiredBytes +
                100L * 1024L * 1024L
            ) {
                throw Exception("Недостаточно свободного места")
            }

            var downloaded = currentBytes
            val buffer = ByteArray(BUFFER_SIZE)

            conn.inputStream.use { input ->
                FileOutputStream(partialFile, append).use { output ->

                    while (true) {
                        if (cancelled) {
                            listenerCancelled(listener)
                            return
                        }

                        val count = input.read(buffer)

                        if (count == -1) break

                        output.write(buffer, 0, count)
                        downloaded += count

                        val percent =
                            ((downloaded * 100) / totalBytes)
                                .toInt()
                                .coerceIn(0, 100)

                        listenerProgress(
                            listener,
                            downloaded,
                            totalBytes,
                            percent
                        )
                    }

                    output.flush()
                }
            }

            if (cancelled) {
                listenerCancelled(listener)
                return
            }

            if (partialFile.length() != totalBytes) {
                throw Exception("Размер файла не совпадает")
            }

            if (!verifyModel(partialFile)) {
                partialFile.delete()
                throw Exception(
                    "Проверка SHA-256 не пройдена"
                )
            }

            if (cancelled) {
                listenerCancelled(listener)
                return
            }
            // Preserve the old file until the verified replacement is installed.
            val backupFile = File(appContext.filesDir, "$MODEL_NAME.backup")
            val hadOldModel = modelFile.exists()
            if (hadOldModel) {
                if (backupFile.exists() && !backupFile.delete()) {
                    throw Exception("Не удалось очистить резервную копию модели")
                }
                if (!modelFile.renameTo(backupFile)) {
                    throw Exception("Не удалось сохранить резервную копию модели")
                }
            }

            if (!partialFile.renameTo(modelFile)) {
                if (hadOldModel && !backupFile.renameTo(modelFile)) {
                    throw Exception("Не удалось сохранить модель и восстановить резервную копию")
                }
                throw Exception("Не удалось сохранить модель")
            }

            if (hadOldModel) {
                backupFile.delete()
            }

            listenerComplete(listener, modelFile)

        } catch (e: Exception) {
            if (cancelled) {
                listenerCancelled(listener)
            } else {
                listenerError(
                    listener,
                    e.message ?: "Ошибка загрузки"
                )
            }
        } finally {
            connection?.disconnect()
            connection = null
        }
    }

    /**
     * Recover a verified model left in the backup slot if the process died
     * between moving the old model aside and installing its replacement.
     * Never overwrite a valid installed model with a backup.
     */
    private fun recoverInterruptedReplacement() {
        val backup = File(appContext.filesDir, "$MODEL_NAME.backup")
        if (!backup.isFile) return

        if (modelFile.isFile && verifyModel(modelFile)) {
            // Installed model is already valid; the backup is obsolete.
            backup.delete()
            return
        }

        if (!verifyModel(backup)) {
            // An unverified backup must never become the active model.
            return
        }

        if (modelFile.exists() && !modelFile.delete()) {
            throw Exception("Не удалось удалить повреждённую модель перед восстановлением")
        }
        if (!backup.renameTo(modelFile)) {
            throw Exception("Не удалось восстановить модель из резервной копии")
        }
    }

    private fun verifyModel(file: File): Boolean {
        val digest = MessageDigest.getInstance("SHA-256")

        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(BUFFER_SIZE)

            while (true) {
                if (cancelled) {
                    throw java.io.InterruptedIOException("Проверка модели отменена")
                }
                val count = input.read(buffer)
                if (count == -1) break
                digest.update(buffer, 0, count)
            }
        }

        val hash = digest.digest().joinToString("") {
            "%02x".format(it)
        }

        return hash.equals(
            EXPECTED_SHA256,
            ignoreCase = true
        )
    }

    private fun listenerProgress(
        listener: Listener,
        downloaded: Long,
        total: Long,
        percent: Int
    ) {
        mainHandler.post {
            listener.onProgress(downloaded, total, percent)
        }
    }

    private fun listenerComplete(
        listener: Listener,
        file: File
    ) {
        mainHandler.post {
            listener.onComplete(file)
        }
    }

    private fun listenerError(
        listener: Listener,
        message: String
    ) {
        mainHandler.post {
            listener.onError(message)
        }
    }

    private fun listenerCancelled(listener: Listener) {
        mainHandler.post {
            listener.onCancelled()
        }
    }
}
