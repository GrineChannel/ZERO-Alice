package com.zeroproject.alice.chat
data class AliceMessageDraft(val role: AliceMessageRole, val text: String, val timestamp: Long = System.currentTimeMillis())
