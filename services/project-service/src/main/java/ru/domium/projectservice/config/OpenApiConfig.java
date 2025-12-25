package ru.domium.projectservice.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI projectServiceOpenAPI() {
        Info info = generateApiInfo();
        SecurityScheme securityScheme = generateSecurityScheme();
        SecurityRequirement securityRequirement = new SecurityRequirement()
                .addList("bearer-jwt");

        return new OpenAPI()
                .info(info)
                .components(new Components()
                        .addSecuritySchemes("bearer-jwt", securityScheme))
                .addSecurityItem(securityRequirement);
    }

    private static SecurityScheme generateSecurityScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("JWT токен из Keycloak через API Gateway");
    }

    private Info generateApiInfo() {
        Contact contact = new Contact();
        contact.setEmail("support@domium.ru");
        contact.setName("Domium Support");

        License license = new License()
                .name("Apache 2.0")
                .url("https://www.apache.org/licenses/LICENSE-2.0.html");

        String description = """
                Project Service — каталог проектов ИЖС и точка входа для заказа строительства.
                Публичные возможности: просмотр/поиск проектов, карточка проекта (Инфо и фото).
                Клиентские возможности (CLIENT): выбор проекта и создание заказа.
                Административные возможности (MANAGER): создание и сопровождение проектов.
                """;

        return new Info()
                .title("Project Service API")
                .version("1.0.0")
                .contact(contact)
                .description(description)
                .license(license);
    }
}
