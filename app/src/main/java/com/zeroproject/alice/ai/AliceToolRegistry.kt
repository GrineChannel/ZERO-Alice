package com.zeroproject.alice.ai

enum class AliceToolPermission {
    READ_ONLY,
    USER_APPROVAL
}

data class AliceToolDescriptor(
    val id: String,
    val description: String,
    val permission: AliceToolPermission,
    val capabilities: Set<AliceCapability>
)

/**
 * Describes potential tools, not executable permissions.
 * Actual implementations must validate permissions again at execution time.
 */
class AliceToolRegistry {
    private val tools = linkedMapOf<String, AliceToolDescriptor>()

    fun register(tool: AliceToolDescriptor): Boolean {
        require(tool.id.isNotBlank()) { "Tool ID cannot be blank" }
        if (tools.containsKey(tool.id)) return false
        tools[tool.id] = tool
        return true
    }

    fun find(id: String): AliceToolDescriptor? = tools[id]

    fun forCapability(capability: AliceCapability): List<AliceToolDescriptor> =
        tools.values.filter { capability in it.capabilities }

    fun all(): List<AliceToolDescriptor> = tools.values.toList()
}
