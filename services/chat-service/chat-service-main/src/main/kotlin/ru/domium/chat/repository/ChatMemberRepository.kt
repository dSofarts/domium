package ru.domium.chat.repository

import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.data.relational.core.query.Criteria.where
import org.springframework.stereotype.Repository
import ru.domium.chat.entity.ChatMember
import ru.domium.chat.repository.base.BaseRepository
import java.util.UUID

@Repository
class ChatMemberRepository(
    override val entityTemplate: R2dbcEntityTemplate,
) : BaseRepository(entityTemplate = entityTemplate) {
    suspend fun getByUserIdAndChatId(
        userId: UUID,
        chatId: UUID,
    ): ChatMember? =
        entityTemplate
            .select(ChatMember::class.java)
            .matching(
                where(ChatMember::chatId.name)
                    .`is`(chatId)
                    .and(where(ChatMember::userId.name).`is`(userId))
                    .toQuery(),
            ).one()
            .awaitFirstOrNull()
}
