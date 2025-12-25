package ru.domium.chat.api.response

import java.util.UUID

data class GetChatResponse(
    val chatId: UUID,
    val projectId: UUID,
    val lastMessage: String? = null,
)
