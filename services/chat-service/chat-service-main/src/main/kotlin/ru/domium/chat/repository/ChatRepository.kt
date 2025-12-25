package ru.domium.chat.repository

import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.data.r2dbc.core.awaitExists
import org.springframework.data.relational.core.query.Criteria.where
import org.springframework.data.relational.core.query.Query
import org.springframework.stereotype.Repository
import ru.domium.chat.entity.Chat
import ru.domium.chat.repository.base.BaseRepository
import java.util.UUID

@Repository
class ChatRepository(
    override val entityTemplate: R2dbcEntityTemplate,
) : BaseRepository(entityTemplate = entityTemplate) {
    suspend fun isExistsByProjectId(projectId: UUID): Boolean =
        entityTemplate
            .select(Chat::class.java)
            .matching(where(Chat::projectId.name).`is`(projectId).toQuery())
            .awaitExists()

    suspend fun getByProjectId(projectId: UUID): Chat? =
        entityTemplate
            .select(Chat::class.java)
            .matching(where(Chat::projectId.name).`is`(projectId).toQuery())
            .one()
            .awaitFirstOrNull()
}
