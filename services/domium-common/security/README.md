# Domium Common Security

Модуль безопасности для всех доменных сервисов Domium.

## Описание

Этот модуль предоставляет готовую конфигурацию безопасности для доменных сервисов:
- Локальная валидация JWT через `issuer-uri`
- Базовая аутентификация на уровне URL
- Поддержка проверки ролей через `@PreAuthorize`
- Утилиты для работы с JWT токенами

## Использование

#### Публикация в Maven Local

```bash
cd services/domium-common
./gradlew :security:publishToMavenLocal
```

### 2. Подключение в сервисе

В `build.gradle` сервиса уже настроено использование Nexus:

```gradle
repositories {
    mavenLocal()
    maven {
        url = "http://localhost:8081/repository/maven-public/"
    }
    mavenCentral()
}

dependencies {
    implementation 'ru.domium:security:1.0.0'
}
```


### 3. Настройка application.yaml

В `application.yaml` сервиса добавьте:

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${KEYCLOAK_ISSUER_URI:http://keycloak:8080/realms/domium}
```

**Примечание:** Проверка публичных путей выполняется на стороне gateway, поэтому в сервисах все запросы (кроме `/actuator/**`) требуют аутентификации.

### 4. Использование в контроллерах

```java
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import ru.domium.security.util.SecurityUtils;

@RestController
@RequestMapping("/api/buildings")
public class BuildingController {

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('BUILDER')")
    public ResponseEntity<BuildingDto> getBuilding(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = SecurityUtils.getCurrentUserId(jwt);
        // ...
    }
}
```

## Автоматическая конфигурация

Модуль использует Spring Boot Auto-Configuration и автоматически настраивается при наличии:
- Зависимостей `spring-boot-starter-security` и `spring-boot-starter-oauth2-resource-server`
- Настройки `spring.security.oauth2.resourceserver.jwt.issuer-uri`

## Компоненты

- **SecurityConfig** - базовая конфигурация безопасности с JWT resource server
- **JwtRoleConverter** - преобразование ролей из Keycloak в Spring Security authorities
- **SecurityUtils** - утилиты для работы с JWT токенами

## Переопределение конфигурации

Если нужно переопределить конфигурацию безопасности, создайте свой `SecurityConfig` с аннотацией `@Configuration` и `@Primary`, или отключите auto-configuration:

```yaml
spring:
  autoconfigure:
    exclude:
      - ru.domium.security.config.DomiumSecurityAutoConfiguration
```

