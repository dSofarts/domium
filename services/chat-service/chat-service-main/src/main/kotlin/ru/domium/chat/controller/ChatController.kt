package ru.domium.chat.controller

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.domium.chat.api.request.CreateChatRequest
import ru.domium.chat.api.response.GetChatResponse
import ru.domium.chat.core.context.DomiumChatStandardHeaders.USER_ID
import ru.domium.chat.core.util.withMdcContext
import ru.domium.chat.service.ChatService
import java.util.UUID

@RestController
@RequestMapping("/chat")
class ChatController(
    private val chatService: ChatService,
) {
    @PostMapping("/create")
    suspend fun createChat(
        @RequestHeader(name = USER_ID, required = true) userId: UUID,
        @RequestBody request: CreateChatRequest,
    ): GetChatResponse =
        withMdcContext {
            logger.info { "Получен запрос на создание чата для проекта ${request.projectId}" }
            return@withMdcContext chatService.createChat(userId, request)
        }

    @GetMapping("/{projectId}")
    suspend fun getChat(
        @RequestHeader(name = USER_ID, required = true) userId: UUID,
        @PathVariable projectId: UUID,
    ): GetChatResponse =
        withMdcContext {
            logger.info { "Получен запрос на получение чата для проекта $projectId" }
            return@withMdcContext chatService.getChatByProject(userId, projectId)
        }

    companion object {
        private val logger = KotlinLogging.logger {}
    }
}
