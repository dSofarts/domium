# DOMIUM APPLICATION

Платформа для управления строительными проектами: веб‑клиент, API Gateway, набор доменных сервисов
(проекты, стройка, документы), а также видеопотоки по объектам.

**Стек**: Java 21, Spring, Postgres, Minio, Docker Compose, Grafana + Prometheus + Loki, Keycloak.
**UI стек**: Next.js 16, React 19, TypeScript, Tailwind CSS, Radix UI, Next Themes.

---
### Содержание
- Архитектура
- Состав репозитория
- Быстрый старт
- Порты и сервисы
- Видео и стриминг
- Наблюдаемость
- Тестирование
---
### Архитектура
Основной поток: `Domium UI` обращается к `API Gateway`, который маршрутизирует запросы в доменные сервисы.
Видеопотоки поднимаются в `building-service` и отдаются через `video-nginx` в HLS формате.

### Диаграммы архитектуры (C4 + PlantUML)

PlantUML схемы находятся в `docs/`:
- **Context (уровень системы)** - [docs/context.puml](docs/context.puml)

  ![Context (уровень системы)](docs/context.png)

- **Container (уровень контейнеров)** - [docs/containers.puml](docs/containers.puml)

  ![Container (уровень контейнеров)](docs/containers.png)

---
### Состав репозитория
```
domium/
├── services/                бэкенд
│   ├── api-gateway/
│   ├── project-service/
│   ├── building-service/
│   ├── document-service/
│   ├── chat-service/
├── domium-ui/                фронтенд
├── infra/                    конфигурации мониторинга, Keycloak/Minio/Postgres, видео и RTSP-тесты
├── docs/                     диаграммы
├─ docker-compose.yaml        инфраструктура + сервисы
```

---
### Быстрый старт
> Требуется: **Docker** (compose), **JDK 21** (если запускать сервисы локально из IDE).
> Порты по умолчанию: 8080 (Keycloak), 8090 (API Gateway), 8091 (Building Service), 8092 (Document Service), 8095 (Project Service), 9090 (Prometheus), 3000 (Grafana), 3100 (Loki), 3000 (Domium UI), 8088 (Video Nginx), 8554 (RTSP).

1. **Подготовка окружения**
   ```bash
   copy .env.example .env
   ```

2. **Запуск всего через Docker**
   ```bash
   docker compose up -d
   ```
   запуск со сборкой сервисов:
   ```bash
   docker compose up --build -d
   ```

3. **Запуск сервисов из IDE/CLI (локально, без Docker)**

    * Building Service (building-service):

      ```bash
      cd services/building-service
      ./gradlew :app:bootRun   # порт 8091
      ```

---
### Сервисы

| Сервис           | Порт (host) | Описание                     | Адрес                  |
|------------------|-------------|------------------------------|------------------------|
| Domium UI        | 3000        | Контейнер `domium-ui`        | http://localhost:3000/ |
| API Gateway      | 8090        | Gateway                      | http://localhost:8090/ |
| Keycloak         | 8080        | Dev-мод                      | http://localhost:8080/ |
| Postgres         | 5432        | Контейнер `postgres`         |                        |
| Building Service | 8091        | Контейнер `building-service` |                        |
| Document Service | 8092        | Контейнер `document-service` |                        |
| Project Service  | 8095        | Контейнер `project-service`  |                        |
| Consul           | 8500        | Service Discovery            | http://localhost:8500/ |
| Minio            | 9000/9001   | Хранилище документов + Loki  | http://localhost:9001/ |
| Prometheus       | 9090        | Метрики                      | http://localhost:9090/ |
| Grafana          | 3000        | Dashboard + Explore          | http://localhost:3000/ |
| Loki             | 3100        | Хранилище логов              |                        |
| Alloy            | 9080/4317   | Сбор логов/трейсов/метрик     | http://localhost:9080/ |
| Postgres Exporter| 9187        | Метрики Postgres             | http://localhost:9187/ |
| Video Nginx      | 8088        | HLS и тестовая страница      | http://localhost:8088/ |
| RTSP Server      | 8554/8888   | Тестовый RTSP источник       | rtsp://localhost:8554/test |

---

### API Документация (Swagger)

- **Агрегированная документация всех сервисов**: 
  - Swagger UI: http://localhost:8090/swagger-ui.html
  - OpenAPI JSON: http://localhost:8090/v3/api-docs/aggregated

### Авторизация в Swagger UI

Для выполнения запросов к защищенным API:
1. Получите JWT токен из Keycloak (см. [API Gateway README](services/api-gateway/README.md#авторизация-в-swagger-ui))
2. В Swagger UI нажмите кнопку **"Authorize"** и введите токен
3. Все запросы будут выполняться с авторизацией


Prometheus уже сконфигурирован на сбор `/actuator/prometheus`. Grafana **автопровиженит** датасорсы Prometheus/Loki/Tempo.

---

### Видео и стриминг
- **RTSP тестовый источник**: `rtsp://localhost:8554/test` (поднимается `rtsp-server` + `rtsp-publisher-file`)
- **HLS выдача** через `video-nginx`: `http://localhost:8088/hls/{buildingId}/{cameraId}/index.m3u8`
- **Старт потока** (same‑origin через nginx): `GET http://localhost:8088/api/video/stream?buildingId=...&cameraId=...`

### Наблюдаемость

* **Метрики**: `http://localhost:8090/actuator/prometheus`
* **Prometheus**: `http://localhost:9090`
* **Grafana**: `http://localhost:3000` (admin/admin). Датасорсы Prometheus/Loki провиженятся из `infra/grafana/provisioning`.
* **Логи**: Loki развёрнут; для доставки логов приложений используется `grafana/alloy` (конфиг в `infra/alloy`).
---

## Состав команды
- [Александр Мунченко](https://github.com/AlexBarin): реализация document-service
- [Виталий Король](https://github.com/korolvd): архитектура проекта, реализация building-service, api-gateway
- [Кирилл Ларкин](https://github.com/KiryaLar): реализация project-service
- [Анастасия Филина](https://github.com/Arvantis): тестирование
- [Антон Родионов](https://github.com/stvdent47): аналитика
- [Егор Дронов](https://github.com/dSofarts): реализация domium-ui, chat-service