package com.zeroproject.alice.ai

sealed class AliceToolResult {
    data class Success(val output: String) : AliceToolResult()
    data class Rejected(val reason: String) : AliceToolResult()
    data class Failure(val error: String) : AliceToolResult()
}

interface AliceToolHandler {
    val id: String
    fun execute(input: String): AliceToolResult
}

/** Explicit approval is required for every non-read-only invocation. */
class AliceToolExecutor(private val registry: AliceToolRegistry) {
    private val handlers = mutableMapOf<String, AliceToolHandler>()

    @Synchronized
    fun register(handler: AliceToolHandler) {
        require(registry.find(handler.id) != null) { "Tool must be declared first" }
        handlers[handler.id] = handler
    }

    @Synchronized
    fun execute(id: String, input: String, approved: Boolean = false): AliceToolResult {
        val descriptor = registry.find(id)
            ?: return AliceToolResult.Rejected("Unknown tool")
        if (descriptor.permission == AliceToolPermission.USER_APPROVAL && !approved) {
            return AliceToolResult.Rejected("User approval required")
        }
        val handler = handlers[id] ?: return AliceToolResult.Rejected("Tool not connected")
        return try {
            handler.execute(input)
        } catch (e: Exception) {
            AliceToolResult.Failure(e.message ?: "Tool execution failed")
        }
    }
}
