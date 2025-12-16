package ru.domium.chat.core.exception

class ChatAlreadyExistException(
    debugMessage: String,
    params: Map<String, String>,
) : DomiumChatException(
        type = DomiumChatExceptionType.CHAT_ALREADY_EXIST_EXCEPTION,
        debugMessage = debugMessage,
        debugParams = params,
    )
