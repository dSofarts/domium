# DOMIUM APPLICATION

**Стек**: Java 21, Spring, Kafka, Postgres, Redis, Minio, Docker Compose, Grafana + Prometheus + Loki, Keycloak.

---
## Содержание
* Архитектура
* Состав репозитория
* Быстрый старт
* Порты и сервисы
* Наблюдаемость
* Тестирование
---
## Архитектура
## Диаграммы архитектуры (C4 + PlantUML)

PlantUML схемы находятся в `docs/`:
- **Context (уровень системы)** - [docs/context.puml](docs/context.puml)

  ![Context (уровень системы)](docs/context.png)

- **Container (уровень контейнеров)** - [docs/containers.puml](docs/containers.puml)

  ![Container (уровень контейнеров)](docs/containers.png)

---
## Состав репозитория
```
domium/
├── services/                бэкенд
│   ├── api-gateway/
│   ├── project-service/
│   ├── building-service/
│   ├── document-service/
│   ├── chat-service/
├── domium-ui/                фронтенд
├── infra/                    конфигурации Grafana/Prometheus/Tempo/Loki/Keycloak и инициализация бд.
├── docs/                     диаграммы
├─ docker-compose.yml         инфраструктура + сервисы
```

---
## Быстрый старт
> Требуется: **Docker** (compose), **JDK 21**, . Порты по умолчанию: 8080 (Keycloak), 8091 (Building Service), 9090 (Prometheus), 3000 (Grafana), 3100 (Loki).

   ```bash
   docker compose up -d
   ```
   запуск со сборкой сервисов:

```bash
    docker compose up --build -d
   ```
---

2. **Запуск сервисов из IDE/CLI (локально, без Docker)**

    * Building Service:

      ```bash
      cd services/building-service
      ./gradlew bootRun   # порт 8091
      ```

---
## Сервисы

| Сервис             | Порт (host) | Описание                     | Адрес                         |
|--------------------|-------------|------------------------------|-------------------------------|
| Keycloak           | 8080        | Dev-мод                      | http://localhost:8080/        |
| Postgres           | 5432        | Контейнер `postgres`         |                               |
| Building Service   | 8091        | Контейнер `building-service` |                               |
| Redis              | 6379        | Кеширование                  |                               |
| Redis insight      | 5540        | ui                           | http://localhost:5540/        |
| Kafka              | 9092        | Шина                         |                               |
| Kafka-UI           | 8070        | ui                           | http://localhost:8070/        |
| Prometheus         | 9090        | Метрики                      | http://localhost:9090/        |
| Grafana            | 3000        | Dashboard + Explore          | http://localhost:3000/        |
| Loki               | 3100        | Хранилище логов              |                               |
| Minio              | 9000        | Хранилище документов + Loki  | http://localhost:9001/        |


Prometheus уже сконфигурирован на сбор `/actuator/prometheus`. Grafana **автопровиженит** датасорсы Prometheus/Loki/Tempo.

---

## Наблюдаемость

* **Метрики**: `http://localhost:8090/actuator/prometheus`
* **Prometheus**: `http://localhost:9090`
* **Grafana**: `http://localhost:3000` (admin/admin). Датасорсы Prometheus/Loki провиженятся из `infra/grafana/provisioning`.
* **Логи**: Loki развёрнут; для доставки логов приложений используйте promtail или logback‑аппендер для Loki (в репозитории базовая конфигурация Loki уже есть).
---

## Тестирование

todo

---
