package ru.domium.chat.core.util

import org.springframework.web.reactive.function.server.ServerRequest
import ru.domium.chat.core.context.MdcContextVariableName.MDC_CONTEXT

val ServerRequest.mdcContext: Map<String, String?>?
    get() = attribute(MDC_CONTEXT).orElse(null) as? Map<String, String?>
