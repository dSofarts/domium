package ru.domium.chat.core.configuration

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.domium.chat.core.context.DomiumChatContextService
import ru.domium.chat.core.context.DomiumContextInitializer

@Configuration(proxyBeanMethods = false)
class ContextConfiguration {
    @Bean
    @ConditionalOnMissingBean
    fun contextService(): DomiumChatContextService = DomiumChatContextService

    @Bean
    @ConditionalOnMissingBean
    fun contextInitializer(contextService: DomiumChatContextService): DomiumContextInitializer = DomiumContextInitializer(contextService)
}
