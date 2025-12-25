package ru.domium.chat.core.context

import org.slf4j.MDC
import ru.domium.chat.core.context.MdcContextVariableName.CALL_ID
import ru.domium.chat.core.context.MdcContextVariableName.INITIATOR_SERVICE
import ru.domium.chat.core.context.MdcContextVariableName.METHOD
import ru.domium.chat.core.context.MdcContextVariableName.UNDEFINED
import ru.domium.chat.core.context.MdcContextVariableName.USER_ID
import java.util.UUID
import java.util.function.BiConsumer
import java.util.function.Function

interface MdcService {
    fun fillMdc(
        url: String,
        method: String,
        headerGetter: Function<String, String?>,
    )

    fun fillHeaders(headerSetter: BiConsumer<String, String>)

    companion object Default : MdcService {
        override fun fillMdc(
            url: String,
            method: String,
            headerGetter: Function<String, String?>,
        ) {
            MDC.clear()

            setMdc(CALL_ID, uuid())
            setMdc(METHOD, "${url}_$method")
            setMdc(USER_ID, headerGetter.apply(DomiumChatStandardHeaders.USER_ID))
            setMdc(INITIATOR_SERVICE, headerGetter.apply(DomiumChatStandardHeaders.INITIATOR_SERVICE))
        }

        override fun fillHeaders(headerSetter: BiConsumer<String, String>) {
            setHeader(USER_ID, DomiumChatStandardHeaders.USER_ID, headerSetter)
        }

        private fun setHeader(
            key: String,
            headerName: String,
            setter: BiConsumer<String, String>,
        ) {
            MDC.get(key)?.let { if (it.isNotEmpty() && it != UNDEFINED) setter.accept(headerName, it) }
        }

        private fun setMdc(
            key: String,
            value: String?,
        ) {
            MDC.get(key).let { if (it == null && value != null && value != UNDEFINED) MDC.put(key, value) }
        }

        internal fun uuid(): String = UUID.randomUUID().toString()
    }
}
