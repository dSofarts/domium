package ru.domium.chat.core.handler

import ru.domium.chat.api.response.DomiumChatErrorResponse
import ru.domium.chat.core.exception.DomiumChatException
import ru.domium.chat.core.exception.DomiumChatExceptionType
import ru.domium.chat.core.exception.UnexpectedException

interface ExceptionSerializationService {
    fun serialize(exception: DomiumChatException): ExceptionSerializationResult<out Any>
}

class DefaultExceptionSerializationService : ExceptionSerializationService {
    override fun serialize(exception: DomiumChatException) =
        ExceptionSerializationResult(
            body =
                DomiumChatErrorResponse(
                    type = exception.type.name,
                    debugMessage = exception.debugMessage,
                    debugParams =
                        when {
                            exception.shouldIncludeDebugParams() -> exception.constructDebugParams()
                            else -> null
                        },
                ),
            httpStatus =
                when (exception) {
                    is UnexpectedException -> exception.httpStatus ?: exception.type.status
                    else -> exception.type.status
                },
        )

    private fun DomiumChatException.shouldIncludeDebugParams(): Boolean = this.type != DomiumChatExceptionType.UNEXPECTED_EXCEPTION

    private fun DomiumChatException.constructDebugParams(): Map<String, Any?> {
        val debugParams = this.debugParams.toMutableMap()

        debugParams["exception"] = this.javaClass.name
        debugParams["location"] = this.stackTrace?.firstOrNull()?.toString()

        return debugParams
    }
}
