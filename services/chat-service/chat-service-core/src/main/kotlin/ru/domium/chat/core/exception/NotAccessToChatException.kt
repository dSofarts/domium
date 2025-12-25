package ru.domium.chat.core.exception

class NotAccessToChatException(
    chatId: String,
    userId: String,
) : DomiumChatException(
        type = DomiumChatExceptionType.NOT_ACCESS_TO_CHAT_EXCEPTION,
        debugMessage = "User have not been access to chat",
        debugParams =
            mapOf(
                "chatId" to chatId,
                "userId" to userId,
            ),
    )
