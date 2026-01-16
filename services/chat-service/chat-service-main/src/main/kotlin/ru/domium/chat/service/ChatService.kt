package ru.domium.chat.service

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.springframework.aot.hint.TypeReference.listOf
import org.springframework.stereotype.Service
import ru.domium.chat.api.request.CreateChatRequest
import ru.domium.chat.api.response.GetChatResponse
import ru.domium.chat.core.exception.NotAccessToChatException
import ru.domium.chat.core.exception.ObjectNotFoundException
import ru.domium.chat.entity.Chat
import ru.domium.chat.entity.ChatMember
import ru.domium.chat.entity.Message
import ru.domium.chat.repository.ChatMemberRepository
import ru.domium.chat.repository.ChatRepository
import ru.domium.chat.repository.MessageRepository
import java.util.UUID

@Service
class ChatService(
    private val chatRepository: ChatRepository,
    private val chatMemberRepository: ChatMemberRepository,
    private val messageService: MessageService,
) {
    suspend fun createChat(
        userId: UUID,
        request: CreateChatRequest,
    ): GetChatResponse {
        logger.info { "Начато создание чата для проекта ${request.projectId}" }
        if (chatRepository.isExistsByProjectId(request.projectId)) {
            logger.info { "Уже найден существующий чат для данного проекта" }
            val chat =
                chatRepository.getByProjectId(request.projectId)
                    ?: throw ObjectNotFoundException.chat(request.projectId.toString())
            val membersToInsert = mutableListOf<ChatMember>()
            if (chatMemberRepository.getByUserIdAndChatId(userId, chat.id) == null) {
                membersToInsert.add(
                    ChatMember(
                        id = UUID.randomUUID(),
                        chatId = chat.id,
                        userId = userId,
                        username = request.userName,
                    ),
                )
            }
            if (chatMemberRepository.getByUserIdAndChatId(request.managerId, chat.id) == null) {
                membersToInsert.add(
                    ChatMember(
                        id = UUID.randomUUID(),
                        chatId = chat.id,
                        userId = request.managerId,
                        username = request.managerName,
                    ),
                )
            }
            if (membersToInsert.isNotEmpty()) {
                chatMemberRepository.insert(list = membersToInsert, clazz = ChatMember::class)
            }
            val lastMessage = messageService.getLastMessageByChatId(chatId = chat.id)
            return GetChatResponse(
                chatId = chat.id,
                projectId = chat.projectId,
                lastMessage = lastMessage?.content,
            )
        }

        val chat =
            chatRepository
                .insert(
                    item =
                        Chat(
                            id = UUID.randomUUID(),
                            projectId = request.projectId,
                        ),
                    clazz = Chat::class,
                ).also {
                    logger.info { "Успешно создан новый чат ${it.id}" }
                }
        logger.info { "Начато добавление пользователей к чату" }
        val members =
            listOf(
                ChatMember(
                    id = UUID.randomUUID(),
                    chatId = chat.id,
                    userId = userId,
                    username = request.userName,
                ),
                ChatMember(
                    id = UUID.randomUUID(),
                    chatId = chat.id,
                    userId = request.managerId,
                    username = request.managerName,
                ),
            )
        chatMemberRepository.insert(list = members, clazz = ChatMember::class).also {
            logger.info { "Пользователи успешно добавлены в чат" }
        }

        return GetChatResponse(
            chatId = chat.id,
            projectId = request.projectId,
        )
    }

    suspend fun getChatByProject(
        userId: UUID,
        projectId: UUID,
    ): GetChatResponse {
        logger.info { "Начат поиск чата по проекту $projectId" }
        val chat = chatRepository.getByProjectId(projectId) ?: throw ObjectNotFoundException.chat(projectId.toString())
        logger.info { "Найден чат ${chat.id} для проекта $projectId" }
        checkAccess(chat.id.toString(), userId.toString())
        val lastMessage = messageService.getLastMessageByChatId(chatId = chat.id)
        return GetChatResponse(
            chatId = chat.id,
            projectId = chat.projectId,
            lastMessage = lastMessage?.content,
        )
    }

    suspend fun checkAccess(
        chatId: String,
        userId: String,
    ) {
        logger.info { "Начата проверка доступа пользователя $userId к чату $chatId" }
        chatRepository.getRequiredObjectById(id = UUID.fromString(chatId), clazz = Chat::class).also {
            logger.debug { "Найден чат с id ${it.id}" }
        }
        chatMemberRepository.getByUserIdAndChatId(UUID.fromString(userId), UUID.fromString(chatId))
            ?: throw NotAccessToChatException(
                chatId,
                userId,
            )
    }

    companion object {
        private val logger = KotlinLogging.logger {}
    }
}
