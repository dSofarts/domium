package ru.domium.chat.core.context

import java.util.function.BiConsumer
import java.util.function.Function

interface DomiumChatContextService {
    fun fillContext(
        headerGetter: Function<String, String?>,
        headerSetter: BiConsumer<String, String>,
    )

    companion object DefaultDomiumChatContextService : DomiumChatContextService {
        override fun fillContext(
            headerGetter: Function<String, String?>,
            headerSetter: BiConsumer<String, String>,
        ) {
            headerGetter.apply(DomiumChatStandardHeaders.USER_ID)?.let {
                headerSetter.accept(DomiumChatStandardHeaders.USER_ID, it)
            }
            headerGetter.apply(DomiumChatStandardHeaders.INITIATOR_SERVICE)?.let {
                headerSetter.accept(DomiumChatStandardHeaders.INITIATOR_SERVICE, it)
            }
        }
    }
}
