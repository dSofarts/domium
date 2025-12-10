package ru.domium.chat.exception

import org.springframework.http.HttpStatus

enum class DomiumChatExceptionType(
    val status: HttpStatus,
) {
    OBJECT_NOT_FOUND(HttpStatus.NOT_FOUND),
    UNAUTHORIZED_EXCEPTION(HttpStatus.UNAUTHORIZED),
    FORBIDDEN_EXCEPTION(HttpStatus.FORBIDDEN),
    BAD_REQUEST_EXCEPTION(HttpStatus.BAD_REQUEST),
    UNEXPECTED_EXCEPTION(HttpStatus.INTERNAL_SERVER_ERROR),
}
