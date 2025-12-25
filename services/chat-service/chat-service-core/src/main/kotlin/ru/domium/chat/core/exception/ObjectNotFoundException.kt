package ru.domium.chat.core.exception

import kotlin.reflect.KClass

class ObjectNotFoundException(
    type: DomiumChatExceptionType = DomiumChatExceptionType.OBJECT_NOT_FOUND_EXCEPTION,
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

        fun chat(projectId: String) =
            ObjectNotFoundException(
                message = "Chat does not exist",
                parameters =
                    mapOf(
                        "projectId" to projectId,
                    ),
            )

        fun chatMember(
            chatId: String,
            userId: String,
        ) = ObjectNotFoundException(
            message = "User not access to chat",
            parameters =
                mapOf(
                    "chatId" to chatId,
                    "userId" to userId,
                ),
        )
    }
}
