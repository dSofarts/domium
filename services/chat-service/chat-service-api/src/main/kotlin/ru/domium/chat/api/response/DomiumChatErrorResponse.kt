package ru.domium.chat.api.response

data class DomiumChatErrorResponse(
    val type: String,
    val debugMessage: String,
    val debugParams: Map<String, Any?>?,
)
