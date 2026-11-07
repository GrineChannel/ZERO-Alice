
package com.zeroproject.alice

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.StatFs
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.Executors

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
        fun onProgress(
            downloaded: Long,
            total: Long,
            percent: Int
        )

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

    @Volatile
    private var cancelled = false

    @Volatile
    private var connection: HttpURLConnection? = null

    fun getModelFile(): File = modelFile

    fun isModelDownloaded(): Boolean {
        return modelFile.exists() &&
            modelFile.isFile &&
            modelFile.length() > 0
    }

    fun start(listener: Listener) {
        if (executor.isShutdown) {
            listenerError(listener, "Загрузчик уже закрыт")
            return
        }

        cancelled = false

        executor.execute {
            download(listener)
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
            if (isModelDownloaded()) {
                if (verifyModel(modelFile)) {
                    listenerComplete(listener, modelFile)
                    return
                }

                modelFile.delete()
            }

            val existingBytes =
                if (partialFile.exists()) partialFile.length()
                else 0L

            val url = URL(MODEL_URL)

            val conn =
                (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 30000
                    readTimeout = 60000
                    instanceFollowRedirects = true
                    requestMethod = "GET"
                    setRequestProperty("User-Agent", "ZERO-Alice-Android")
                    if (existingBytes > 0) {
                        setRequestProperty(
                            "Range",
                            "bytes=$existingBytes-"
                        )
                    }
                }

            connection = conn

            conn.connect()

            if (cancelled) {
                listenerCancelled(listener)
                return
            }

            val responseCode = conn.responseCode

            val append: Boolean
            val totalBytes: Long

            when (responseCode) {
                HttpURLConnection.HTTP_PARTIAL -> {
                    append = true

                    val contentRange =
                        conn.getHeaderField("Content-Range")

                    totalBytes =
                        contentRange
                            ?.substringAfter("/")
                            ?.toLongOrNull()
                            ?: throw Exception(
                                "Сервер не сообщил полный размер файла"
                            )
                }

                HttpURLConnection.HTTP_OK -> {
                    append = false
                    totalBytes = conn.contentLengthLong
                    if (existingBytes > 0) {
                        partialFile.delete()
                    }
                }

                else -> {
                    throw Exception(
                        "Ошибка сервера: HTTP $responseCode"
                    )
                }
            }

            if (totalBytes <= 0) {
                throw Exception("Не удалось определить размер модели")
            }

            val remainingBytes =
                totalBytes -
                    if (append) existingBytes else 0L

            val availableBytes =
                StatFs(appContext.filesDir.path).availableBytes

            if (availableBytes < remainingBytes + 100L * 1024L * 1024L) {
                throw Exception(
                    "Недостаточно свободного места для загрузки"
                )
            }

            var downloaded =
                if (append) existingBytes else 0L

            val buffer = ByteArray(BUFFER_SIZE)

            conn.inputStream.use { input ->
                partialFile.outputStream()
                    .buffered()
                    .use { output ->

                        if (append) {
                            output.close()
                            partialFile.appendBytes(byteArrayOf())
                        }

                        val fileOutput =
                            if (append) {
                                partialFile.appendOutputStream()
                            } else {
                                partialFile.outputStream()
                            }

                        fileOutput.buffered().use { stream ->
                            while (true) {
                                if (cancelled) {
                                    listenerCancelled(listener)
                                    return
                                }

                                val count = input.read(buffer)

                                if (count == -1) break

                                stream.write(buffer, 0, count)

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
                        }
                    }
            }

            if (cancelled) {
                listenerCancelled(listener)
                return
            }

            if (!partialFile.exists() ||
                partialFile.length() != totalBytes
            ) {
                throw Exception("Размер загруженного файла не совпадает")
            }

            if (!verifyModel(partialFile)) {
                partialFile.delete()
                throw Exception(
                    "Проверка SHA-256 не пройдена. Файл повреждён."
                )
            }

            if (modelFile.exists()) {
                modelFile.delete()
            }

            if (!partialFile.renameTo(modelFile)) {
                throw Exception("Не удалось сохранить модель")
            }

            listenerComplete(listener, modelFile)

        } catch (e: Exception) {
            if (cancelled) {
                listenerCancelled(listener)
            } else {
                listenerError(
                    listener,
                    e.message ?: "Неизвестная ошибка загрузки"
                )
            }
        } finally {
            connection?.disconnect()
            connection = null
        }
    }

    private fun verifyModel(file: File): Boolean {
        val digest = MessageDigest.getInstance("SHA-256")

        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(BUFFER_SIZE)

            while (true) {
                val count = input.read(buffer)
                if (count == -1) break

                digest.update(buffer, 0, count)
            }
        }

        val actualHash =
            digest.digest().joinToString("") {
                "%02x".format(it)
            }

        return actualHash.equals(
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
