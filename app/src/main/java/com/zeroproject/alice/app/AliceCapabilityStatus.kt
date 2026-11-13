package com.zeroproject.alice.app
enum class AliceCapabilityStatus { READY, NEEDS_SETUP, PLANNED, UNAVAILABLE }
data class AliceCapability(val feature: AliceFeature, val status: AliceCapabilityStatus, val details: String)
