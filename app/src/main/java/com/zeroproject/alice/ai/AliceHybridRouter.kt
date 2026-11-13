package com.zeroproject.alice.ai

/** Routing policy only. It does not transmit data to any provider. */
class AliceHybridRouter(
    private val intelligence: AliceIntelligenceCore = AliceIntelligenceCore()
) {
    fun route(
        request: String,
        online: Boolean,
        privacy: AlicePrivacyPolicy,
        explicitCloudConsent: Boolean
    ): AliceTask {
        val cloudAllowed = privacy.maySendText(explicitCloudConsent)
        return intelligence.createTask(
            request = request,
            cloudAvailable = online,
            cloudAllowed = cloudAllowed
        )
    }
}
