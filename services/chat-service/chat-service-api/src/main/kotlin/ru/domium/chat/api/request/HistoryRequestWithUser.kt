package ru.domium.chat.api.request

import java.time.LocalDateTime

data class HistoryRequestWithUser(
    val userId: String,
    val chatId: String,
    val beforeTimestamp: LocalDateTime,
    val limit: Int = 50,
)
