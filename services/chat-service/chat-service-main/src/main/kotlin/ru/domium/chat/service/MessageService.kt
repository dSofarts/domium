package ru.domium.chat.service

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import org.springframework.stereotype.Service
import ru.domium.chat.entity.Message
import ru.domium.chat.repository.MessageRepository
import java.time.LocalDateTime
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Service
class MessageService(
    private val messageRepository: MessageRepository,
) {
    private val messagesFlows: MutableMap<UUID, MutableSharedFlow<Message>> = ConcurrentHashMap()

    suspend fun getLastMessageByChatId(chatId: UUID): Message? = messageRepository.getLastMessageByChatId(chatId)

    suspend fun sendMessage(
        userId: String,
        chatId: String,
        content: String,
    ): Message {
        val message =
            Message(
                id = UUID.randomUUID(),
                chatId = UUID.fromString(chatId),
                senderId = UUID.fromString(userId),
                content = content,
            )
        messageRepository.insert(item = message, clazz = Message::class)
        getFlowForChat(UUID.fromString(chatId)).emit(message)
        return message
    }

    fun subscribe(chatId: String): Flow<Message> = getFlowForChat(UUID.fromString(chatId))

    suspend fun getHistory(chatId: String): List<Message> =
        messageRepository.getLastMessages(UUID.fromString(chatId), LocalDateTime.now(), MAX_MESSAGES)

    suspend fun loadMoreHistory(
        chatId: String,
        beforeTimestamp: LocalDateTime,
        limit: Int,
    ): List<Message> =
        messageRepository
            .getLastMessages(UUID.fromString(chatId), beforeTimestamp, limit)

    fun getFlowForChat(chatId: UUID): MutableSharedFlow<Message> =
        messagesFlows.computeIfAbsent(chatId) {
            MutableSharedFlow(replay = REPLAY)
        }

    companion object {
        private val logger = KotlinLogging.logger {}
        private const val MAX_MESSAGES = 50
        private const val REPLAY = 100
    }
}
