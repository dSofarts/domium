# project-service

Сервис управления проектами Domium.

## Назначение
`project-service` отвечает за доменную область проектов:
- проекты (Project);
- помещения/комнаты (Room);
- этажи (Floor);
- заказы проекта (ProjectOrder) и их позиции (ProjectItem);
- назначения исполнителям (OrderAssignment);
- изображения проекта (ProjectImage) и работа с объектным хранилищем (S3/MinIO).

Сервис является частью микросервисной системы и обычно запускается совместно с инфраструктурой из корня репозитория (`docker-compose.yaml`).

## Технологии
- Java + Spring Boot
- Spring Web (REST)
- Spring Data JPA + Hibernate
- Liquibase (миграции БД)
- OpenAPI/Swagger (springdoc)
- Object Storage: S3/MinIO
- Логирование: SLF4J/Logback

## Требования
- JDK (версия соответствует настройкам Gradle проекта)
- Docker (для локального окружения с Postgres/MinIO/Keycloak и т.д.)

## Конфигурация
- `src/main/resources/application.yaml`

Миграции Liquibase:
- `src/main/resources/db/changelog/db.changelog-master.yaml`
- `src/main/resources/db/changelog/changes/*.yaml`

## Запуск

### В составе всего окружения
Рекомендуемый способ для локальной разработки — поднять инфраструктуру из корня репозитория:
- `docker-compose.yaml`

### Локально (только сервис)
1. Убедитесь, что зависимости (БД, MinIO и т.п.) доступны.
2. Запустите приложение:
- через IDE (класс `ru.domium.projectservice.ProjectServiceApplication`)
- или через Gradle:
  - `./gradlew bootRun`

## Документация API (Swagger)
После запуска Swagger UI доступен по стандартному пути springdoc (зависит от настроек), обычно:
- `/swagger-ui.html` или `/swagger-ui/index.html`

## Структура пакетов
- `ru.domium.projectservice.controller` — REST контроллеры
- `ru.domium.projectservice.service` — бизнес-логика
- `ru.domium.projectservice.repository` — JPA репозитории
- `ru.domium.projectservice.entity` — JPA сущности
- `ru.domium.projectservice.dto` — DTO запросов/ответов
- `ru.domium.projectservice.mapper` — мапперы (MapStruct/ручные)
- `ru.domium.projectservice.exception` — исключения и обработчики
- `ru.domium.projectservice.objectstorage` — интеграция с S3/MinIO

## Безопасность и доступы
Сервис предполагает работу за API Gateway/Keycloak.
Доступ к эндпоинтам ограничивается ролями (например, MANAGER и др.) согласно аннотациям безопасности и описаниям в `@Operation`.


