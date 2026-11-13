
package com.zeroproject.alice.engineer.minecraft

import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

enum class ValidationSeverity {
    ERROR,
    WARNING,
    INFO
}

data class ValidationIssue(
    val severity: ValidationSeverity,
    val file: String,
    val message: String
)

data class MinecraftValidationResult(
    val valid: Boolean,
    val issues: List<ValidationIssue>
)

class AliceMinecraftValidator {

    fun validateProject(
        project: MinecraftProject
    ): MinecraftValidationResult {

        val issues = mutableListOf<ValidationIssue>()

        val behaviorPack = File(
            project.directory,
            "behavior_pack"
        )

        val resourcePack = File(
            project.directory,
            "resource_pack"
        )

        validatePack(
            behaviorPack,
            "behavior_pack",
            issues
        )

        if (resourcePack.exists()) {
            validatePack(
                resourcePack,
                "resource_pack",
                issues
            )
        }

        validateJavaScript(
            behaviorPack,
            "behavior_pack",
            issues
        )

        val hasBehaviorManifest = File(
            behaviorPack,
            "manifest.json"
        ).isFile

        if (!hasBehaviorManifest) {
            issues.add(
                ValidationIssue(
                    ValidationSeverity.ERROR,
                    "behavior_pack/manifest.json",
                    "Behavior Pack не содержит manifest.json"
                )
            )
        }

        val hasResourceManifest = File(
            resourcePack,
            "manifest.json"
        ).isFile

        if (resourcePack.exists() && !hasResourceManifest) {
            issues.add(
                ValidationIssue(
                    ValidationSeverity.WARNING,
                    "resource_pack/manifest.json",
                    "Resource Pack существует, но manifest.json отсутствует"
                )
            )
        }

        val hasErrors = issues.any {
            it.severity == ValidationSeverity.ERROR
        }

        return MinecraftValidationResult(
            valid = !hasErrors,
            issues = issues
        )
    }

    private fun validatePack(
        directory: File,
        relativePath: String,
        issues: MutableList<ValidationIssue>
    ) {

        if (!directory.exists()) {
            issues.add(
                ValidationIssue(
                    ValidationSeverity.ERROR,
                    relativePath,
                    "Папка пакета не найдена"
                )
            )
            return
        }

        if (!directory.isDirectory) {
            issues.add(
                ValidationIssue(
                    ValidationSeverity.ERROR,
                    relativePath,
                    "Путь пакета не является папкой"
                )
            )
            return
        }

        val manifest = File(directory, "manifest.json")

        if (!manifest.isFile) return

        try {

            val json = JSONObject(
                manifest.readText(Charsets.UTF_8)
            )

            validateManifest(
                json,
                "$relativePath/manifest.json",
                issues,
                directory
            )

        } catch (error: Throwable) {

            issues.add(
                ValidationIssue(
                    ValidationSeverity.ERROR,
                    "$relativePath/manifest.json",
                    "Ошибка чтения JSON: ${error.message}"
                )
            )
        }
    }

    private fun validateManifest(
        json: JSONObject,
        path: String,
        issues: MutableList<ValidationIssue>,
        packRoot: File
    ) {

        val header = json.optJSONObject("header")

        if (header == null) {
            issues.add(
                ValidationIssue(
                    ValidationSeverity.ERROR,
                    path,
                    "Отсутствует объект header"
                )
            )
            return
        }

        val headerUuid = header.optString("uuid")

        if (!isValidUuid(headerUuid)) {
            issues.add(
                ValidationIssue(
                    ValidationSeverity.ERROR,
                    path,
                    "Некорректный UUID в header.uuid"
                )
            )
        }

        val version = header.optJSONArray("version")

        if (!isValidVersion(version)) {
            issues.add(
                ValidationIssue(
                    ValidationSeverity.ERROR,
                    path,
                    "header.version должен содержать три неотрицательных числа"
                )
            )
        }

        val name = header.optString("name")

        if (name.isBlank()) {
            issues.add(
                ValidationIssue(
                    ValidationSeverity.ERROR,
                    path,
                    "Не указано название пакета"
                )
            )
        }

        val modules = json.optJSONArray("modules")

        if (modules == null || modules.length() == 0) {
            issues.add(
                ValidationIssue(
                    ValidationSeverity.ERROR,
                    path,
                    "Отсутствуют модули пакета"
                )
            )
            return
        }

        val moduleUuids = mutableSetOf<String>()

        for (i in 0 until modules.length()) {

            val module = modules.optJSONObject(i)

            if (module == null) {
                issues.add(
                    ValidationIssue(
                        ValidationSeverity.ERROR,
                        path,
                        "Модуль $i должен быть объектом"
                    )
                )
                continue
            }

            val uuid = module.optString("uuid")

            if (!isValidUuid(uuid)) {
                issues.add(
                    ValidationIssue(
                        ValidationSeverity.ERROR,
                        path,
                        "Некорректный UUID модуля $i"
                    )
                )
            } else if (!moduleUuids.add(uuid)) {
                issues.add(
                    ValidationIssue(
                        ValidationSeverity.ERROR,
                        path,
                        "Повторяющийся UUID модуля: $uuid"
                    )
                )
            }

            val type = module.optString("type")

            if (type !in setOf("data", "resources", "script")) {
                issues.add(
                    ValidationIssue(
                        ValidationSeverity.ERROR,
                        path,
                        "Недопустимый тип модуля: $type"
                    )
                )
            }

            if (!isValidVersion(module.optJSONArray("version"))) {
                issues.add(
                    ValidationIssue(
                        ValidationSeverity.ERROR,
                        path,
                        "Некорректная версия модуля $i"
                    )
                )
            }

            if (type == "script") {
                validateScriptModule(module, path, issues)
                val entry = module.optString("entry")
                if (entry.isNotBlank() && isSafeScriptEntry(entry) && packRoot.isDirectory) {
                    val entryFile = File(packRoot, entry).canonicalFile
                    val canonicalPack = packRoot.canonicalFile
                    if (!entryFile.path.startsWith(canonicalPack.path + File.separator) ||
                        !entryFile.isFile
                    ) {
                        issues.add(ValidationIssue(
                            ValidationSeverity.ERROR,
                            path,
                            "Script Module entry не найден: $entry"
                        ))
                    }
                }
            }
        }

        if (headerUuid in moduleUuids) {
            issues.add(
                ValidationIssue(
                    ValidationSeverity.ERROR,
                    path,
                    "UUID header и модуля должны различаться"
                )
            )
        }

        validateDependencies(
            json.optJSONArray("dependencies"),
            path,
            issues
        )
    }

    private fun isSafeScriptEntry(entry: String): Boolean {
        return !entry.startsWith("/") && !entry.contains('\\') &&
            !entry.contains(':') &&
            entry.split('/').all { it.isNotBlank() && it != "." && it != ".." }
    }

    private fun validateScriptModule(
        module: JSONObject,
        path: String,
        issues: MutableList<ValidationIssue>
    ) {

        val language = module.optString("language")

        if (language.isNotBlank() && language != "javascript") {
            issues.add(
                ValidationIssue(
                    ValidationSeverity.ERROR,
                    path,
                    "Script Module должен использовать javascript"
                )
            )
        }

        val entry = module.optString("entry")

        if (entry.isBlank()) {
            issues.add(
                ValidationIssue(
                    ValidationSeverity.ERROR,
                    path,
                    "Для Script Module не указан entry"
                )
            )
        } else if (
            entry.startsWith("/") ||
            entry.contains("\\") ||
            entry.split('/').any {
                it.isBlank() || it == "." || it == ".."
            }
        ) {
            issues.add(
                ValidationIssue(
                    ValidationSeverity.ERROR,
                    path,
                    "Недопустимый путь Script Module entry"
                )
            )
        }
    }

    private fun validateDependencies(
        dependencies: JSONArray?,
        path: String,
        issues: MutableList<ValidationIssue>
    ) {

        if (dependencies == null) return

        val seen = mutableSetOf<String>()

        for (i in 0 until dependencies.length()) {

            val dependency = dependencies.optJSONObject(i)

            if (dependency == null) {
                issues.add(
                    ValidationIssue(
                        ValidationSeverity.ERROR,
                        path,
                        "Зависимость $i должна быть объектом"
                    )
                )
                continue
            }

            val uuid = dependency.optString("uuid")
            val moduleName = dependency.optString("module_name")
            val hasUuid = uuid.isNotBlank()
            val hasModuleName = moduleName.isNotBlank()

            if (hasUuid == hasModuleName) {
                issues.add(
                    ValidationIssue(
                        ValidationSeverity.ERROR,
                        path,
                        "Зависимость $i должна содержать либо uuid, либо module_name"
                    )
                )
                continue
            }

            val key = if (hasUuid) {
                if (!isValidUuid(uuid)) {
                    issues.add(
                        ValidationIssue(
                            ValidationSeverity.ERROR,
                            path,
                            "Некорректный UUID зависимости $i"
                        )
                    )
                }
                "uuid:$uuid"
            } else {
                if (!moduleName.startsWith("@minecraft/")) {
                    issues.add(
                        ValidationIssue(
                            ValidationSeverity.WARNING,
                            path,
                            "Неизвестное имя API-модуля: $moduleName"
                        )
                    )
                }
                "module:$moduleName"
            }

            if (!seen.add(key)) {
                issues.add(
                    ValidationIssue(
                        ValidationSeverity.ERROR,
                        path,
                        "Повторяющаяся зависимость: $key"
                    )
                )
            }

            val validVersion = if (hasUuid) {
                isValidVersion(dependency.optJSONArray("version"))
            } else {
                val version = dependency.optString("version")
                version.matches(Regex("\\d+\\.\\d+\\.\\d+(?:-(?:beta|alpha|rc)(?:\\.\\d+)?)?"))
            }
            if (!validVersion) {
                issues.add(
                    ValidationIssue(
                        ValidationSeverity.ERROR,
                        path,
                        "Некорректная версия зависимости $i"
                    )
                )
            }
        }
    }

    private fun validateJavaScript(
        directory: File,
        relativePath: String,
        issues: MutableList<ValidationIssue>
    ) {

        if (!directory.isDirectory) return

        val root = directory.canonicalFile
        val files = directory.walkTopDown().onEnter { folder ->
            (folder.canonicalPath == root.path ||
                folder.canonicalPath.startsWith(root.path + File.separator)) &&
                !java.nio.file.Files.isSymbolicLink(folder.toPath())
        }.filter {
            it.isFile && it.extension.equals("js", ignoreCase = true) &&
                !java.nio.file.Files.isSymbolicLink(it.toPath()) &&
                it.canonicalPath.startsWith(root.path + File.separator)
        }.take(201).toList()
        if (files.size > 200) {
            issues.add(ValidationIssue(
                ValidationSeverity.WARNING,
                relativePath,
                "Проверка JavaScript ограничена первыми 200 файлами"
            ))
        }

        for (file in files) {

            val content = try {
                file.readText(Charsets.UTF_8)
            } catch (error: Throwable) {
                issues.add(
                    ValidationIssue(
                        ValidationSeverity.ERROR,
                        file.relativeTo(directory).path,
                        "Не удалось прочитать JavaScript: ${error.message}"
                    )
                )
                continue
            }

            if (content.isBlank()) {
                issues.add(
                    ValidationIssue(
                        ValidationSeverity.WARNING,
                        file.relativeTo(directory).path,
                        "JavaScript-файл пустой"
                    )
                )
            }

            if (content.contains("import ") &&
                content.contains("from \"@minecraft/server\"") &&
                !File(directory, "manifest.json").isFile
            ) {
                issues.add(ValidationIssue(
                    ValidationSeverity.WARNING,
                    file.relativeTo(directory).invariantSeparatorsPath,
                    "Используется Script API, но manifest.json отсутствует"
                ))
            }

            if (content.contains("require(\"@minecraft/server\")")) {
                issues.add(ValidationIssue(
                    ValidationSeverity.WARNING,
                    file.relativeTo(directory).invariantSeparatorsPath,
                    "Для Bedrock Script API обычно нужен ES module import вместо require()"
                ))
            }

            if (content.contains("eval(")) {
                issues.add(
                    ValidationIssue(
                        ValidationSeverity.WARNING,
                        file.relativeTo(directory).path,
                        "Обнаружен eval(): требуется дополнительная проверка"
                    )
                )
            }
        }
    }

    private fun isValidUuid(value: String): Boolean {
        return try {
            UUID.fromString(value)
            true
        } catch (_: IllegalArgumentException) {
            false
        }
    }

    private fun isValidVersion(value: JSONArray?): Boolean {

        if (value == null || value.length() != 3) {
            return false
        }

        for (i in 0 until 3) {
            if (value.optInt(i, -1) < 0) {
                return false
            }
        }

        return true
    }
}
