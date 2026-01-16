package ru.domium.chat.api.request

data class ChatAccessRequest(
    val userId: String,
    val chatId: String,
)
