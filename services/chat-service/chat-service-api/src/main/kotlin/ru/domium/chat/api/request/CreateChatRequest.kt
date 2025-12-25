package ru.domium.chat.api.request

import java.util.UUID

data class CreateChatRequest(
    val userName: String,
    val managerId: UUID,
    val managerName: String,
    val projectId: UUID,
)
