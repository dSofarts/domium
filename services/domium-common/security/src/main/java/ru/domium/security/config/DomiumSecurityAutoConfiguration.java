package ru.domium.security.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

/**
 * Автоматическая конфигурация для модуля безопасности Domium.
 * Автоматически настраивает JWT resource server при наличии зависимостей.
 */
@AutoConfiguration
@ConditionalOnClass(EnableWebSecurity.class)
@ConditionalOnProperty(
    prefix = "spring.security.oauth2.resourceserver.jwt",
    name = "issuer-uri"
)
public class DomiumSecurityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public SecurityConfig securityConfig() {
        return new SecurityConfig();
    }
}

