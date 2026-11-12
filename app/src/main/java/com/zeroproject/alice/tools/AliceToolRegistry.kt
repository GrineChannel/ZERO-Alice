package com.zeroproject.alice.tools

import com.zeroproject.alice.device.AliceDeviceActions

/**
 * A minimal registry for explicit, user-initiated tools.
 * No tool is executed from model-generated text.
 */
class AliceToolRegistry(
    private val deviceActions: AliceDeviceActions
) {
    data class Tool(
        val id: String,
        val title: String,
        val category: String
    )

    data class Result(
        val handled: Boolean,
        val succeeded: Boolean,
        val message: String
    )

    fun availableTools(): List<Tool> =
        deviceActions.availableActions().map {
            Tool(it.id, it.title, "device")
        }

    fun executeUserCommand(text: String): Result {
        val id = deviceActions.resolveCommand(text)
            ?: return Result(false, false, "")
        val tool = availableTools().firstOrNull { it.id == id }
            ?: return Result(false, false, "")
        val success = deviceActions.execute(tool.id)
        return Result(
            handled = true,
            succeeded = success,
            message = if (success) {
                "Открываю: ${tool.title}."
            } else {
                "Не удалось выполнить: ${tool.title}."
            }
        )
    }
}
