package ru.domium.chat.core.context

import org.springframework.http.HttpHeaders
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import reactor.util.context.Context

class DomiumContextInitializer(
    private val contextService: DomiumChatContextService,
) : WebFilter {
    override fun filter(
        exchange: ServerWebExchange,
        chain: WebFilterChain,
    ): Mono<Void> =
        chain
            .filter(exchange)
            .contextWrite { it.fillContext(exchange.request.headers) }

    private fun Context.fillContext(headers: HttpHeaders): Context =
        apply {
            contextService.fillContext(
                headerGetter = { headerName -> headers.getFirst(headerName) },
                headerSetter = { headerName, headerValue -> this.setHeader(headerName, headerValue) },
            )
        }

    private fun Context.setHeader(
        headerName: String,
        headerValue: String,
    ): Context = this.put(headerName, headerValue)
}
