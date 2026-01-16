### API Gateway Service

API Gateway на основе Spring Cloud Gateway с интеграцией Keycloak для аутентификации и авторизации.

### Функциональность

- **JWT аутентификация** через Keycloak
- **Авторизация по ролям** (CLIENT, MANAGER)
- **Маршрутизация** на микросервисы
- **Глобальные фильтры** для добавления заголовков пользователя
- **CORS** поддержка
- **Метрики** Prometheus
- **Swagger UI** с агрегацией документации всех микросервисов

### Конфигурация

### Переменные окружения

- `KEYCLOAK_ISSUER_URI` - URI Keycloak realm (по умолчанию: `http://keycloak.localhost:8080/realms/domium`)
- `KEYCLOAK_INTERNAL_URL` - Внутренний URL Keycloak для получения JWKS (по умолчанию: `http://keycloak.localhost:8080`)
- `CONSUL_HOST` - хост Consul (по умолчанию: `localhost`)
- `CONSUL_PORT` - порт Consul (по умолчанию: `8500`)

### Маршруты

- `/api/projects/**` → `project-service` (требует роль CLIENT или MANAGER)
- `/api/buildings/**` → `building-service` (требует роль CLIENT или MANAGER)
- `/ws/chat/**` → `chat-service` (WebSocket)



**Примечание:** Роли определяются через JWT токен. Сервисы получают роли через JWT (resource-server) и/или проксируемые заголовки gateway (если включено).

### Запуск

```bash
./gradlew bootRun
```

Порт по умолчанию: `8090`

---

### API Документация (Swagger)

API Gateway предоставляет единую точку доступа к документации всех микросервисов через Swagger UI.

### Агрегированная документация

- **Swagger UI**: http://localhost:8090/swagger-ui.html
  - Отображает объединенную документацию всех зарегистрированных в Consul микросервисов
  - Позволяет переключаться между общей документацией и документацией отдельных сервисов

- **OpenAPI JSON (агрегированный)**: http://localhost:8090/v3/api-docs/aggregated
  - JSON-представление объединенной документации всех сервисов


**Примечание:** Доступ к Swagger UI и API документации не требует аутентификации (настроено в `SecurityConfig`).

### Авторизация в Swagger UI

Для выполнения запросов к защищенным API через Swagger UI:

1. **Получите токен через Keycloak:**
   
   **В Postman:**
   - Метод: `POST`
   - URL: `http://localhost:8080/realms/domium/protocol/openid-connect/token`
   - Headers: `Content-Type: application/x-www-form-urlencoded`
   - Body (x-www-form-urlencoded):
     - `username`: `test-client`
     - `password`: `change_me`
     - `grant_type`: `password`
     - `client_id`: `domium`
     - `client_secret`: `FaxzBgk7pkyattBrV8MlVCVg80jjZKo5`

2. **В Swagger UI:**
   - Нажмите кнопку **"Authorize"** в правом верхнем углу
   - В поле **"bearer-jwt"** вставьте значение `access_token` из ответа Keycloak
   - Нажмите **"Authorize"** и **"Close"**

3. **Теперь все запросы будут выполняться с авторизацией**

**Пример ответа от Keycloak:**
```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expires_in": 300,
  "refresh_expires_in": 1800,
  "token_type": "Bearer"
}
```

