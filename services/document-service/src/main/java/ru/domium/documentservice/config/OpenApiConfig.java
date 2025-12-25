package ru.domium.documentservice.config;


import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  private static final String SECURITY_SCHEME_NAME = "bearerAuth";

  @Bean
  public OpenAPI documentServiceOpenAPI() {
    return new OpenAPI()
        .info(apiInfo())
        .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
        .components(securityComponents());
  }

  private Info apiInfo() {
    return new Info()
        .title("Document Service API")
        .description("""
                Сервис документооборота строительного проекта.

                Возможности:
                - управление документами
                - версии файлов
                - подписание и отклонение
                - роли CLIENT / BUILDER / ADMIN
                """)
        .version("1.0.0");
  }

  private Components securityComponents() {
    return new Components()
        .addSecuritySchemes(
            SECURITY_SCHEME_NAME,
            new SecurityScheme()
                .name(SECURITY_SCHEME_NAME)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .in(SecurityScheme.In.HEADER)
        );
  }
}