package ru.domium.chat.core.exception

import org.springframework.http.HttpStatus

class UnexpectedException(
    type: DomiumChatExceptionType = DomiumChatExceptionType.UNEXPECTED_EXCEPTION,
    message: String,
    cause: Throwable? = null,
    val httpStatus: HttpStatus?,
    val url: String?,
) : DomiumChatException(
        type = type,
        debugMessage = message,
        debugParams =
            mapOf(
                "url" to url,
            ),
        cause = cause,
    )
