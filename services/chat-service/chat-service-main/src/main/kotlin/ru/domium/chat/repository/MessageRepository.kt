package ru.domium.chat.repository

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.data.domain.Sort.by
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.data.relational.core.query.Criteria.where
import org.springframework.stereotype.Repository
import ru.domium.chat.entity.Message
import ru.domium.chat.repository.base.BaseRepository
import java.time.LocalDateTime
import java.util.UUID

@Repository
class MessageRepository(
    override val entityTemplate: R2dbcEntityTemplate,
) : BaseRepository(entityTemplate = entityTemplate) {
    suspend fun getLastMessageByChatId(chatId: UUID): Message? =
        entityTemplate
            .select(Message::class.java)
            .matching(
                where("chat_id")
                    .`is`(chatId)
                    .toQuery()
                    .sort(by(Message::createdAt.name).descending())
                    .limit(1),
            ).one()
            .awaitFirstOrNull()

    suspend fun getLastMessages(
        chatId: UUID,
        timestamp: LocalDateTime,
        limit: Int,
    ): List<Message> =
        entityTemplate.databaseClient
            .sql(SELECT_MESSAGES)
            .bind(CHAT_ID_PARAMETER, chatId)
            .bind(TIMESTAMP_PARAMETER, timestamp)
            .bind(LIMIT_PARAMETER, limit)
            .map { row, _ ->
                Message.fromRow(row)
            }.all()
            .asFlow()
            .toList()

    companion object {
        const val LIMIT_PARAMETER = "limit"
        const val CHAT_ID_PARAMETER = "chat_id"
        const val TIMESTAMP_PARAMETER = "timestamp"

        const val SELECT_MESSAGES = """
            SELECT * FROM chat_message
            WHERE chat_id = :${CHAT_ID_PARAMETER} AND timestamp < :${TIMESTAMP_PARAMETER}
            ORDER BY created_at DESC
            LIMIT :${LIMIT_PARAMETER}
        """
    }
}
