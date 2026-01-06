### Domium Common OpenAPI

Модуль OpenAPI/Swagger для всех доменных сервисов Domium.

### Описание

Этот модуль предоставляет готовую конфигурацию OpenAPI для доменных сервисов:
- Автоматическая настройка OpenAPI с JWT security scheme
- Настройка через properties (`domium.openapi.*`)
- Поддержка кастомных title, description, version, server URL
- Дефолтные настройки SpringDoc (пути, сортировка, отключение actuator endpoints)

### Использование

#### Публикация в Maven Local

```bash
cd services/domium-common
./gradlew :openapi:publishToMavenLocal
```

### 2. Подключение в сервисе

В `build.gradle` сервиса:

```gradle
repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation 'ru.domium:openapi:1.0.0'
}
```

В `pom.xml` (для Maven проектов):

```xml
<dependencies>
    <dependency>
        <groupId>ru.domium</groupId>
        <artifactId>openapi</artifactId>
        <version>1.0.0</version>
    </dependency>
</dependencies>
```

### 3. Настройка application.yaml

**Все настройки определяются автоматически!** Настройка `domium.openapi` в `application.yaml` **не требуется**.

Модуль автоматически определяет:
- **title**: `{spring.application.name} API` (например, "buildings API")
- **version**: из `info.version` (берется из версии проекта в build.gradle/pom.xml) или "1.0.0" по умолчанию
- **server-url**: `http://localhost:{server.port}` (например, "http://localhost:8091")
- **server-description**: `{spring.application.name} Server` (например, "buildings Server")
- **description**: не указывается (null), если не задан явно

Если нужно переопределить какие-то значения, добавьте в `application.yaml`:

```yaml
domium:
  openapi:
    enabled: true  # по умолчанию true, можно отключить
    title: My Custom API Title  # переопределить автоматический title
    description: Кастомное описание API  # добавить описание
    version: 2.0.0  # переопределить версию
    server-url: https://api.example.com  # переопределить URL сервера
    server-description: Production Server  # переопределить описание сервера
```

**Дефолтные настройки SpringDoc:**

Модуль автоматически применяет следующие настройки SpringDoc (через `application-defaults.yaml`):

```yaml
springdoc:
  api-docs:
    path: /v3/api-docs
  swagger-ui:
    path: /swagger-ui.html
    enabled: true
    operations-sorter: method
    tags-sorter: alpha
  show-actuator: false
```

Эти настройки можно переопределить в `application.yaml` сервиса при необходимости.

### 4. Использование в контроллерах

OpenAPI автоматически сканирует контроллеры и создает документацию. Используйте стандартные аннотации SpringDoc:

```java
@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "Управление пользователями")
public class UserController {

    @GetMapping("/{id}")
    @Operation(summary = "Получить пользователя", description = "Возвращает информацию о пользователе по ID")
    public ResponseEntity<UserDto> getUser(@PathVariable Long id) {
        // ...
    }
}
```

### Автоматическая конфигурация

Модуль использует Spring Boot Auto-Configuration и автоматически настраивается при наличии:
- Зависимости `springdoc-openapi-starter-webmvc-ui` (включена транзитивно)
- Настройки `domium.openapi.enabled=true` (по умолчанию)

### Компоненты

- **DomiumOpenApiAutoConfiguration** - автоконфигурация OpenAPI
- **DomiumOpenApiProperties** - properties для настройки

### Переопределение конфигурации

Если нужно переопределить конфигурацию OpenAPI, создайте свой `@Bean` с типом `OpenAPI` и аннотацией `@Primary`, или отключите auto-configuration:

```yaml
spring:
  autoconfigure:
    exclude:
      - ru.domium.openapi.config.DomiumOpenApiAutoConfiguration
```

### Security Scheme

Модуль автоматически добавляет JWT security scheme с именем `bearer-jwt`:

```yaml
SecurityScheme:
  type: HTTP
  scheme: bearer
  bearerFormat: JWT
  description: JWT токен из Keycloak через API Gateway
```
