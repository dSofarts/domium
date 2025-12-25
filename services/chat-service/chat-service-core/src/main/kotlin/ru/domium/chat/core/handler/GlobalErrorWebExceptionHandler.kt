package ru.domium.chat.core.handler

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.autoconfigure.web.WebProperties
import org.springframework.boot.autoconfigure.web.reactive.error.AbstractErrorWebExceptionHandler
import org.springframework.boot.web.reactive.error.ErrorAttributes
import org.springframework.context.ApplicationContext
import org.springframework.http.HttpStatus
import org.springframework.http.codec.ServerCodecConfigurer
import org.springframework.web.reactive.function.server.HandlerFunction
import org.springframework.web.reactive.function.server.RequestPredicates
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.RouterFunctions
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Mono
import ru.domium.chat.core.exception.DomiumChatException
import ru.domium.chat.core.exception.UnexpectedException
import ru.domium.chat.core.util.doWithMdc
import ru.domium.chat.core.util.mdcContext
import ru.domium.chat.core.util.restoreMdc

class GlobalErrorWebExceptionHandler(
    errorAttributes: ErrorAttributes,
    resourceProperties: WebProperties.Resources,
    applicationContext: ApplicationContext,
    serverCodecConfigurer: ServerCodecConfigurer,
    val exceptionSerializationService: ExceptionSerializationService,
) : AbstractErrorWebExceptionHandler(errorAttributes, resourceProperties, applicationContext) {
    init {
        this.setMessageWriters(serverCodecConfigurer.writers)
        this.setMessageReaders(serverCodecConfigurer.readers)
    }

    override fun getRoutingFunction(errorAttributes: ErrorAttributes?): RouterFunction<ServerResponse> =
        RouterFunctions.route(
            RequestPredicates.all(),
            HandlerFunction {
                this.handleException(it)
            },
        )

    private fun handleException(request: ServerRequest): Mono<ServerResponse> {
        val mdcScope = request.mdcContext
        val prevCtx = mdcScope?.doWithMdc()

        val exception = getError(request)

        if (exception is DomiumChatException) {
            logger.warn(exception) { ERROR_MSG }
        } else {
            logger.error(exception) { ERROR_MSG }
        }

        val domiumChatException =
            when (exception) {
                is DomiumChatException -> {
                    exception
                }

                else -> {
                    UnexpectedException(
                        message = exception.message ?: ERROR_MSG,
                        cause = exception.cause,
                        httpStatus =
                            when (exception) {
                                is ResponseStatusException -> HttpStatus.valueOf(exception.statusCode.value())
                                else -> null
                            },
                        url = request.uri().toString(),
                    )
                }
            }

        val result = exceptionSerializationService.serialize(domiumChatException)

        mdcScope?.restoreMdc(prevCtx)
        return ServerResponse
            .status(result.httpStatus)
            .contentType(result.contentType)
            .bodyValue(result.body)
    }

    companion object {
        private val logger = KotlinLogging.logger {}
        private const val ERROR_MSG = "Exception occurred during request processing"
    }
}
