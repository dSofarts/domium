package ru.domium.chat.core.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import org.slf4j.MDC

typealias MdcContext = Map<String, String?>

suspend fun <T> withMdcContext(
    mdcContextMap: Map<String, String>? = null,
    func: suspend CoroutineScope.() -> T,
) = coroutineScope {
    when (mdcContextMap) {
        null -> withContext(MDCContext(), func)
        else -> withContext(MDCContext(mdcContextMap), func)
    }
}

internal fun MdcContext.doWithMdc(): MdcContext? {
    val prevCtx = MDC.getCopyOfContextMap()
    MDC.setContextMap(prevCtx)
    return prevCtx
}

internal fun MdcContext.restoreMdc(context: MdcContext?) {
    when (context) {
        null -> MDC.clear()
        else -> MDC.setContextMap(context)
    }
}
