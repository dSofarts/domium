package ru.domium.chat.core.exception

import org.springframework.http.HttpStatus

enum class DomiumChatExceptionType(
    val status: HttpStatus,
) {
    OBJECT_NOT_FOUND_EXCEPTION(HttpStatus.NOT_FOUND),
    CHAT_ALREADY_EXIST_EXCEPTION(HttpStatus.BAD_REQUEST),
    NOT_ACCESS_TO_CHAT_EXCEPTION(HttpStatus.FORBIDDEN),
    UNEXPECTED_EXCEPTION(HttpStatus.INTERNAL_SERVER_ERROR),
}
