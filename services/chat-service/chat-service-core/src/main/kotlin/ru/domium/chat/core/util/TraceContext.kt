package ru.domium.chat.core.util

import org.slf4j.MDC
import ru.domium.chat.core.context.MdcContextVariableName.CALL_ID

object TraceContext {
    val callId: String?
        get() = MDC.get(CALL_ID)
}
