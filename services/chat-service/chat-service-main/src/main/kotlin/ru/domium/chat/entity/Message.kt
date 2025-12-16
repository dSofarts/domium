package ru.domium.chat.entity

import io.r2dbc.spi.Row
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@Table("chat_message")
data class Message(
    @Id val id: UUID,
    val chatId: UUID,
    val senderId: UUID,
    val content: String,
    val isRead: Boolean = false,
    val readAt: LocalDateTime? = null,
    val createdAt: LocalDateTime = LocalDateTime.now(),
) {
    companion object {
        fun fromRow(row: Row): Message =
            Message(
                id = row.get("id", UUID::class.java)!!,
                chatId = row.get("chat_id", UUID::class.java)!!,
                senderId = row.get("sender_id", UUID::class.java)!!,
                content = row.get("content", String::class.java)!!,
                isRead = row.get("is_read", Boolean::class.java)!!,
                readAt = row.get("read_at", LocalDateTime::class.java),
                createdAt = row.get("created_at", LocalDateTime::class.java)!!,
            )
    }
}
