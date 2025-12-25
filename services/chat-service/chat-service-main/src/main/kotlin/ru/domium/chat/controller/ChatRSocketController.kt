package ru.domium.chat.controller

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.stereotype.Controller
import ru.domium.chat.api.request.HistoryRequestWithUser
import ru.domium.chat.api.request.MessageRequest
import ru.domium.chat.entity.Message
import ru.domium.chat.service.ChatService
import ru.domium.chat.service.MessageService

@Controller
class ChatRSocketController(
    private val chatService: ChatService,
    private val messageService: MessageService,
) {
    @MessageMapping("send")
    suspend fun sendMessage(request: MessageRequest): Message {
        chatService.checkAccess(request.chatId, request.userId)
        return messageService.sendMessage(
            userId = request.userId,
            chatId = request.chatId,
            content = request.content,
        )
    }

    @MessageMapping("subscribe")
    fun subscribe(
        chatId: String,
        userId: String,
    ): Flow<Message> {
        runBlocking { chatService.checkAccess(chatId, userId) }
        return messageService.subscribe(chatId)
    }

    @MessageMapping("history")
    suspend fun history(
        chatId: String,
        userId: String,
    ): List<Message> {
        chatService.checkAccess(chatId, userId)
        return messageService.getHistory(chatId)
    }

    @MessageMapping("loadHistory")
    suspend fun loadHistory(request: HistoryRequestWithUser): List<Message> {
        chatService.checkAccess(request.chatId, request.userId)
        return messageService.loadMoreHistory(request.chatId, request.beforeTimestamp, request.limit)
    }
}
