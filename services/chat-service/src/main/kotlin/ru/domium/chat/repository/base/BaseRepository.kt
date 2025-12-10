package ru.domium.chat.repository.base

import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.data.r2dbc.core.allAndAwait
import org.springframework.data.r2dbc.core.awaitExists
import org.springframework.data.relational.core.query.Criteria.where
import org.springframework.data.relational.core.query.CriteriaDefinition
import org.springframework.data.relational.core.query.Query
import org.springframework.data.relational.core.query.Update
import org.springframework.data.relational.core.sql.SqlIdentifier
import org.springframework.stereotype.Repository
import ru.domium.chat.exception.ObjectNotFoundException
import java.util.UUID
import kotlin.reflect.KClass

@Repository
abstract class BaseRepository(
    protected val entityTemplate: R2dbcEntityTemplate,
) {
    suspend fun <T : Any> getById(
        id: UUID,
        clazz: KClass<T>,
    ): T? =
        entityTemplate
            .select(clazz.java)
            .matching(Query.query(where(ID).`is`(id)))
            .one()
            .awaitFirstOrNull()

    suspend fun <T : Any> getRequiredObjectById(
        id: UUID,
        clazz: KClass<T>,
    ): T =
        getById(id, clazz)
            ?: throw ObjectNotFoundException.other(
                id = id.toString(),
                clazz = clazz,
            )

    suspend fun <T : Any> isExistsById(
        id: UUID,
        clazz: KClass<T>,
    ): Boolean =
        entityTemplate
            .select(clazz.java)
            .matching(Query.query(where(ID).`is`(id)))
            .awaitExists()

    suspend fun <T : Any> checkExistById(
        id: UUID,
        clazz: KClass<T>,
    ) {
        if (!isExistsById(id = id, clazz = clazz)) {
            throw ObjectNotFoundException.other(
                id = id.toString(),
                clazz = clazz,
            )
        }
    }

    suspend fun <T : Any> insert(
        item: T,
        clazz: KClass<T>,
    ): T =
        entityTemplate
            .insert(clazz.java)
            .using(item)
            .awaitSingle()

    suspend fun <T : Any> insert(
        list: List<T>,
        clazz: KClass<T>,
    ) {
        list.map {
            insert(item = it, clazz = clazz)
        }
    }

    suspend fun <T : Any> updateById(
        clazz: KClass<T>,
        updates: Map<String, Any?>,
        criteriaId: UUID,
    ): Long =
        entityTemplate
            .update(
                Query.query(where(ID).`is`(criteriaId)),
                Update.from(updates.toAssignments()),
                clazz.java,
            ).awaitSingle()

    suspend fun <T : Any> deleteById(
        id: UUID,
        clazz: KClass<T>,
    ): Long =
        entityTemplate
            .delete(clazz.java)
            .matching(Query.query(where(ID).`is`(id)))
            .allAndAwait()

    suspend fun <T : Any> deleteByIds(
        ids: List<UUID>,
        clazz: KClass<T>,
    ): Long {
        ids.ifEmpty { return 0 }
        return entityTemplate
            .delete(clazz.java)
            .matching(Query.query(where(ID).`in`(ids)))
            .allAndAwait()
    }

    protected fun Map<String, Any?>.toAssignments(): Map<SqlIdentifier, Any?> =
        this.map { SqlIdentifier.unquoted(it.key) to it.value }.toMap()

    protected fun CriteriaDefinition.toQuery(): Query = Query.query(this)

    companion object {
        const val ID = "id"
    }
}
