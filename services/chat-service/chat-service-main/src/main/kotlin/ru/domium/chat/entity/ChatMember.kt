package ru.domium.chat.entity

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime
import java.util.UUID

@Table("chat_member")
data class ChatMember(
    @Id val id: UUID,
    val chatId: UUID,
    val userId: UUID,
    val username: String,
    val joinedAt: LocalDateTime = LocalDateTime.now(),
)
