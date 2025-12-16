package ru.domium.chat.api.request

import java.util.UUID

data class MessageRequest(
    val chatId: String,
    val userId: String,
    val content: String,
)
