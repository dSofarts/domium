package ru.domium.chat.core.configuration

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.boot.autoconfigure.web.WebProperties
import org.springframework.boot.web.reactive.error.ErrorAttributes
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.http.codec.ServerCodecConfigurer
import ru.domium.chat.core.handler.DefaultExceptionSerializationService
import ru.domium.chat.core.handler.ExceptionSerializationService
import ru.domium.chat.core.handler.GlobalErrorWebExceptionHandler

@Configuration(proxyBeanMethods = false)
class WebServiceErrorHandlerStarterConfiguration {
    @Bean
    @ConditionalOnMissingBean(ExceptionSerializationService::class)
    fun exceptionSerializationService(): ExceptionSerializationService = DefaultExceptionSerializationService()

    @Bean
    @Order(-2)
    @ConditionalOnMissingBean
    @ConditionalOnWebApplication
    fun globalErrorWebExceptionHandler(
        errorAttributes: ErrorAttributes,
        webProperties: WebProperties,
        applicationContext: ApplicationContext,
        serverCodecConfigurer: ServerCodecConfigurer,
        exceptionSerializationService: ExceptionSerializationService,
    ) = GlobalErrorWebExceptionHandler(
        errorAttributes = errorAttributes,
        resourceProperties = webProperties.resources,
        applicationContext = applicationContext,
        serverCodecConfigurer = serverCodecConfigurer,
        exceptionSerializationService = exceptionSerializationService,
    )
}
