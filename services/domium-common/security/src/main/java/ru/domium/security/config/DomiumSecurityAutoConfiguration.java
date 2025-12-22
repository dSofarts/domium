package ru.domium.security.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

@AutoConfiguration(beforeName = "org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration")
@ConditionalOnClass(EnableWebSecurity.class)
@ConditionalOnProperty(
    prefix = "spring.security.oauth2.resourceserver.jwt",
    name = "issuer-uri"
)
@EnableConfigurationProperties(DomiumSecurityProperties.class)
@Import(SecurityConfig.class)
public class DomiumSecurityAutoConfiguration {
}

