package ru.domium.chat.exception

import kotlin.reflect.KClass

open class ObjectNotFoundException(
    type: DomiumChatExceptionType = DomiumChatExceptionType.OBJECT_NOT_FOUND,
    message: String,
    parameters: Map<String, Any?> = emptyMap(),
) : DomiumChatException(
        type = type,
        debugMessage = message,
        debugParams = parameters,
    ) {
    companion object {
        fun other(
            id: String,
            clazz: KClass<out Any>,
        ) = ObjectNotFoundException(
            message = "${clazz.simpleName} is not found",
            parameters =
                mapOf(
                    "id" to id,
                    "className" to clazz.simpleName,
                ),
        )
    }
}
