package ru.domium.chat.core.handler

import org.springframework.http.HttpStatus
import org.springframework.http.MediaType

data class ExceptionSerializationResult<T : Any>(
    val body: T,
    val httpStatus: HttpStatus,
    val contentType: MediaType = MediaType.APPLICATION_JSON,
)
