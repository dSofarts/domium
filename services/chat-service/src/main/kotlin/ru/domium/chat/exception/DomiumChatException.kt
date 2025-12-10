package ru.domium.chat.exception

abstract class DomiumChatException(
    val type: DomiumChatExceptionType,
    val debugMessage: String,
    val debugParams: Map<String, Any?> = emptyMap(),
) : RuntimeException(debugMessage)
