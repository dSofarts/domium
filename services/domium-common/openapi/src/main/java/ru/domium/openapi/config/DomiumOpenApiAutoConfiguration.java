package ru.domium.openapi.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.env.Environment;
import org.springframework.context.annotation.Bean;

import java.util.List;

@AutoConfiguration
@ConditionalOnClass(OpenAPI.class)
@ConditionalOnProperty(prefix = "domium.openapi", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(DomiumOpenApiProperties.class)
public class DomiumOpenApiAutoConfiguration {

    public static final String SECURITY_SCHEME_NAME = "bearer-jwt";

    @Bean
    @ConditionalOnMissingBean(OpenAPI.class)
    public OpenAPI domiumOpenAPI(DomiumOpenApiProperties props, Environment env) {
        final String appName = env.getProperty("spring.application.name", "service");

        final String title = props.getTitle() != null ? props.getTitle() : appName + " API";

        final String version = props.getVersion() != null 
            ? props.getVersion() 
            : env.getProperty("info.version", "1.0.0");

        Info info = new Info()
            .title(title)
            .version(version);
        
        if (props.getDescription() != null && !props.getDescription().isBlank()) {
            info.description(props.getDescription());
        }

        SecurityScheme securityScheme = new SecurityScheme()
            .type(SecurityScheme.Type.HTTP)
            .scheme("bearer")
            .bearerFormat("JWT")
            .description("JWT токен из Keycloak через API Gateway");

        SecurityRequirement securityRequirement = new SecurityRequirement().addList(SECURITY_SCHEME_NAME);

        OpenAPI api = new OpenAPI()
            .info(info)
            .components(new io.swagger.v3.oas.models.Components()
                .addSecuritySchemes(SECURITY_SCHEME_NAME, securityScheme))
            .addSecurityItem(securityRequirement);

        String serverPort = env.getProperty("server.port", "8080");
        String serverUrl = props.getServerUrl() != null && !props.getServerUrl().isBlank()
            ? props.getServerUrl()
            : "http://localhost:" + serverPort;
        
        String serverDescription = props.getServerDescription() != null && !props.getServerDescription().isBlank()
            ? props.getServerDescription()
            : appName + " Server";
        
        Server server = new Server()
            .url(serverUrl)
            .description(serverDescription);
        api.setServers(List.of(server));

        return api;
    }
}
