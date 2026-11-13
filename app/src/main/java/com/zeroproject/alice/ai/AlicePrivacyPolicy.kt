package com.zeroproject.alice.ai

/**
 * Default-deny cloud policy. Callers must explicitly authorize each request.
 * Personal data detection is not automatic and must be handled upstream.
 */
data class AlicePrivacyPolicy(
    val cloudEnabled: Boolean = false,
    val allowSendingConversation: Boolean = false,
    val allowSendingFiles: Boolean = false
) {
    fun maySendText(explicitConsent: Boolean): Boolean =
        cloudEnabled && allowSendingConversation && explicitConsent

    fun maySendFile(explicitConsent: Boolean): Boolean =
        cloudEnabled && allowSendingFiles && explicitConsent
}
