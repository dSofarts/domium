package ru.domium.chat.entity

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime
import java.util.UUID

@Table("chat_room")
data class Chat(
    @Id val id: UUID,
    val projectId: UUID,
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
