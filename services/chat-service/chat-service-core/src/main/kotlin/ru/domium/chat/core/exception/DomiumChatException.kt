package ru.domium.chat.core.exception

abstract class DomiumChatException(
    val type: DomiumChatExceptionType,
    val debugMessage: String,
    val debugParams: Map<String, Any?> = emptyMap(),
    cause: Throwable? = null,
) : RuntimeException(debugMessage, cause)
