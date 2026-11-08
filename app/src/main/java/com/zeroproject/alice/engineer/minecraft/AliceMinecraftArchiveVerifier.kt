
package com.zeroproject.alice.engineer.minecraft

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipInputStream

data class MinecraftArchiveVerification(
    val valid: Boolean,
    val checkedPacks: Int,
    val checkedFiles: Int,
    val errors: List<String>
)

class AliceMinecraftArchiveVerifier {

    companion object {
        private const val MAX_PACK_SIZE = 50L * 1024L * 1024L
        private const val MAX_ENTRIES = 1000
        private const val BUFFER_SIZE = 8192
    }

    fun verifyAddon(file: File): MinecraftArchiveVerification {

        val errors = mutableListOf<String>()
        var checkedPacks = 0
        var checkedFiles = 0

        if (!file.isFile || file.length() <= 0L) {
            return MinecraftArchiveVerification(
                false, 0, 0,
                listOf("Файл Add-on отсутствует или пуст")
            )
        }

        try {
            ZipInputStream(file.inputStream().buffered()).use { zip ->

                var entry = zip.nextEntry

                while (entry != null) {

                    if (entry.isDirectory) {
                        entry = zip.nextEntry
                        continue
                    }

                    checkedFiles++

                    if (checkedFiles > MAX_ENTRIES) {
                        errors.add("Слишком много файлов в архиве")
                        break
                    }

                    if (!isSafePath(entry.name)) {
                        errors.add("Недопустимый путь: ${entry.name}")
                        entry = zip.nextEntry
                        continue
                    }

                    if (!entry.name.endsWith(".mcpack", true)) {
                        errors.add(
                            "Неожиданный файл в Add-on: ${entry.name}"
                        )
                        entry = zip.nextEntry
                        continue
                    }

                    val packBytes = readLimited(zip, MAX_PACK_SIZE)

                    if (packBytes == null) {
                        errors.add(
                            "Пакет слишком большой: ${entry.name}"
                        )
                        entry = zip.nextEntry
                        continue
                    }

                    val packErrors = verifyPack(packBytes)

                    if (packErrors.isEmpty()) {
                        checkedPacks++
                    } else {
                        errors.addAll(
                            packErrors.map {
                                "${entry.name}: $it"
                            }
                        )
                    }

                    entry = zip.nextEntry
                }
            }

        } catch (error: Exception) {
            errors.add(
                "Ошибка чтения Add-on: ${error.message ?: "неизвестная ошибка"}"
            )
        }

        if (checkedPacks == 0 && errors.isEmpty()) {
            errors.add("В Add-on не найдено ни одного корректного mcpack")
        }

        return MinecraftArchiveVerification(
            valid = errors.isEmpty() && checkedPacks > 0,
            checkedPacks = checkedPacks,
            checkedFiles = checkedFiles,
            errors = errors
        )
    }

    fun verifyMcpack(file: File): MinecraftArchiveVerification {

        if (!file.isFile || file.length() <= 0L) {
            return MinecraftArchiveVerification(
                false, 0, 0,
                listOf("Файл mcpack отсутствует или пуст")
            )
        }

        if (file.length() > MAX_PACK_SIZE) {
            return MinecraftArchiveVerification(
                false, 0, 0,
                listOf("Размер mcpack превышает лимит")
            )
        }

        return try {
            val errors = verifyPack(file.readBytes())

            MinecraftArchiveVerification(
                valid = errors.isEmpty(),
                checkedPacks = if (errors.isEmpty()) 1 else 0,
                checkedFiles = 0,
                errors = errors
            )
        } catch (error: Exception) {
            MinecraftArchiveVerification(
                false, 0, 0,
                listOf("Ошибка чтения mcpack: ${error.message}")
            )
        }
    }

    private fun verifyPack(bytes: ByteArray): List<String> {

        val errors = mutableListOf<String>()
        var manifestFound = false
        var fileCount = 0

        try {
            ZipInputStream(
                ByteArrayInputStream(bytes)
            ).use { zip ->

                var entry = zip.nextEntry

                while (entry != null) {

                    if (!entry.isDirectory) {

                        fileCount++

                        if (fileCount > MAX_ENTRIES) {
                            errors.add("Слишком много файлов в mcpack")
                            break
                        }

                        if (!isSafePath(entry.name)) {
                            errors.add(
                                "Недопустимый путь: ${entry.name}"
                            )
                        }

                        if (entry.name == "manifest.json") {
                            manifestFound = true
                        }
                    }

                    entry = zip.nextEntry
                }
            }

        } catch (error: Exception) {
            errors.add(
                "Повреждён ZIP-архив: ${error.message}"
            )
        }

        if (!manifestFound) {
            errors.add("В корне пакета отсутствует manifest.json")
        }

        if (fileCount == 0) {
            errors.add("Пакет пуст")
        }

        return errors
    }

    private fun readLimited(
        zip: ZipInputStream,
        limit: Long
    ): ByteArray? {

        val output = ByteArrayOutputStream()
        val buffer = ByteArray(BUFFER_SIZE)
        var total = 0L

        while (true) {

            val count = zip.read(buffer)

            if (count == -1) break

            total += count

            if (total > limit) {
                return null
            }

            output.write(buffer, 0, count)
        }

        return output.toByteArray()
    }

    private fun isSafePath(path: String): Boolean {

        if (path.isBlank()) return false
        if (path.startsWith("/") || path.contains("\\")) return false
        if (path.contains(":")) return false

        return path.split('/').none {
            it.isBlank() || it == "." || it == ".."
        }
    }
}
