package ru.domium.chat.core.filter

import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import ru.domium.chat.core.context.MdcService

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class InitMdcWebFilter : WebFilter {
    override fun filter(
        exchange: ServerWebExchange,
        chain: WebFilterChain,
    ): Mono<Void> =
        chain
            .filter(exchange)
            .contextWrite { ctx ->
                MdcService.fillMdc(
                    url = exchange.request.uri.toString(),
                    method = exchange.request.method.name(),
                    headerGetter = { exchange.request.headers[it]?.firstOrNull() },
                )

                ctx
            }
}
