### Domium Common

Общие библиотеки/стартеры для доменных сервисов Domium.

### Модули

- **`security`**: общий ресурс‑сервер (JWT) + `@PublicEndpoint`
- **`openapi`**: общая конфигурация OpenAPI/Swagger для всех сервисов

### Публикация в Maven Local (для сервисов)

```bash
cd services/domium-common
./gradlew :security:publishToMavenLocal
./gradlew :openapi:publishToMavenLocal
```


