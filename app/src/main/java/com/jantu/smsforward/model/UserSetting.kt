package com.jantu.smsforward.model

data class UserSettings(
    val triggerTexts: List<String> = emptyList(),
    val forwardingPhoneNumber: String = "",
    val forwardingMethod: ForwardingMethod = ForwardingMethod.NONE
)