
package com.zeroproject.alice.engineer.minecraft

import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

data class MinecraftAddonBuildResult(
    val addonFile: File,
    val behaviorPackFile: File?,
    val resourcePackFile: File?,
    val createdAt: Long
)

class AliceMinecraftAddonBuilder(
    private val validator: AliceMinecraftValidator
) {

    companion object {
        private const val MAX_FILES = 500
        private const val MAX_TOTAL_SIZE = 50L * 1024L * 1024L
        private const val BUFFER_SIZE = 8192
    }

    fun build(
        project: MinecraftProject
    ): MinecraftAddonBuildResult {

        val validation = validator.validateProject(project)

        require(validation.valid) {
            val errors = validation.issues
                .filter {
                    it.severity == ValidationSeverity.ERROR
                }
                .joinToString("\n") {
                    "${it.file}: ${it.message}"
                }

            "Проект не прошёл проверку:\n$errors"
        }

        val behaviorPack = File(
            project.directory,
            "behavior_pack"
        )

        val resourcePack = File(
            project.directory,
            "resource_pack"
        )

        require(File(behaviorPack, "manifest.json").isFile) {
            "Behavior Pack не содержит manifest.json"
        }

        val hasResourcePack =
            resourcePack.isDirectory &&
            File(resourcePack, "manifest.json").isFile

        val exportDirectory = File(
            project.directory,
            "exports"
        )

        check(
            exportDirectory.exists() ||
                exportDirectory.mkdirs()
        ) {
            "Не удалось создать папку экспорта"
        }

        val buildId = UUID.randomUUID().toString()

        val behaviorOutput = File(
            exportDirectory,
            "behavior_$buildId.mcpack"
        )

        val resourceOutput = if (hasResourcePack) {
            File(
                exportDirectory,
                "resource_$buildId.mcpack"
            )
        } else {
            null
        }

        val addonOutput = File(
            exportDirectory,
            "${safeFileName(project.name)}_$buildId.mcaddon"
        )

        val temporaryAddon = File(
            exportDirectory,
            "$buildId.tmp"
        )

        try {

            createPackArchive(
                behaviorPack,
                behaviorOutput
            )

            if (resourcePack.isDirectory && hasResourcePack) {
                createPackArchive(
                    resourcePack,
                    resourceOutput!!
                )
            }

            ZipOutputStream(
                BufferedOutputStream(
                    FileOutputStream(temporaryAddon)
                )
            ).use { zip ->

                addFileToZip(
                    zip,
                    behaviorOutput,
                    behaviorOutput.name
                )

                if (resourceOutput != null) {
                    addFileToZip(
                        zip,
                        resourceOutput,
                        resourceOutput.name
                    )
                }
            }

            check(temporaryAddon.renameTo(addonOutput)) {
                "Не удалось завершить создание mcaddon"
            }

            return MinecraftAddonBuildResult(
                addonFile = addonOutput,
                behaviorPackFile = behaviorOutput,
                resourcePackFile = resourceOutput,
                createdAt = System.currentTimeMillis()
            )

        } catch (error: Throwable) {

            temporaryAddon.delete()
            behaviorOutput.delete()
            resourceOutput?.delete()
            addonOutput.delete()

            throw IllegalStateException(
                "Ошибка сборки Minecraft Add-on: ${error.message}",
                error
            )
        }
    }

    private fun createPackArchive(
        packDirectory: File,
        outputFile: File
    ) {

        require(packDirectory.isDirectory) {
            "Папка пакета не найдена"
        }

        val root = packDirectory.canonicalFile

        val files = packDirectory.walkTopDown()
            .filter { it.isFile }
            .toList()

        require(files.isNotEmpty()) {
            "Пакет не содержит файлов"
        }

        require(files.size <= MAX_FILES) {
            "Слишком много файлов в пакете"
        }

        var totalSize = 0L

        for (file in files) {

            val canonical = file.canonicalFile

            require(
                canonical.path.startsWith(
                    root.path + File.separator
                )
            ) {
                "Обнаружен файл за пределами пакета"
            }

            totalSize += file.length()

            require(totalSize <= MAX_TOTAL_SIZE) {
                "Размер пакета превышает допустимый лимит"
            }
        }

        ZipOutputStream(
            BufferedOutputStream(
                FileOutputStream(outputFile)
            )
        ).use { zip ->

            for (file in files) {

                val relativePath = file.relativeTo(packDirectory)
                    .invariantSeparatorsPath

                require(isSafeArchivePath(relativePath)) {
                    "Недопустимый путь внутри архива"
                }

                addFileToZip(
                    zip,
                    file,
                    relativePath
                )
            }
        }
    }

    private fun addFileToZip(
        zip: ZipOutputStream,
        file: File,
        archivePath: String
    ) {

        require(isSafeArchivePath(archivePath)) {
            "Недопустимое имя файла архива"
        }

        zip.putNextEntry(
            ZipEntry(archivePath)
        )

        file.inputStream().buffered().use { input ->

            val buffer = ByteArray(BUFFER_SIZE)

            while (true) {

                val count = input.read(buffer)

                if (count == -1) break

                zip.write(buffer, 0, count)
            }
        }

        zip.closeEntry()
    }

    private fun isSafeArchivePath(path: String): Boolean {

        if (path.isBlank()) return false

        if (path.startsWith("/") || path.contains("\\")) {
            return false
        }

        if (path.contains(":")) return false

        return path.split('/').none {
            it.isBlank() || it == "." || it == ".."
        }
    }

    private fun safeFileName(name: String): String {

        val cleaned = name
            .replace(Regex("""[^\p{L}\p{N}_-]+"""), "_")
            .trim('_')

        return cleaned
            .take(60)
            .ifBlank { "Minecraft_Addon" }
    }
}
