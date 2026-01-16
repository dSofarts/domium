### domium-building

### Сборка/публикация

Публикация API в `mavenLocal` (для других сервисов):

```bash
cd services/domium-building
./gradlew :api:publishToMavenLocal
```

Сборка приложения:

```bash
cd services/domium-building
./gradlew :app:build -x test
```

### Видео с IP-камер (только онлайн HLS)

Схема: **RTSP камера → FFmpeg (внутри domium-building) → HLS на общий volume → nginx раздаёт `/hls`**.

- **HLS**: `GET /buildings/{buildingId}/cameras/{cameraId}/stream` возвращает `hlsUrl` вида `/hls/{buildingId}/{cameraId}/index.m3u8` и (по требованию) стартует FFmpeg.
- **Доступ**: nginx защищён через `auth_request` в `domium-building` (`/internal/video/auth`) и принимает `Authorization: Bearer ...`.

Быстрый старт (docker-compose в корне репозитория):
- В `docker-compose.yaml` добавлены `video-nginx` и volume `video_data`.
- Порт nginx настраивается `VIDEO_NGINX_PORT` (по умолчанию `8088`).

Авто-останов (по неактивности):
- `VIDEO_AUTO_STOP_ENABLED` (default `true`)
- `VIDEO_IDLE_TIMEOUT_SECONDS` (default `120`) — если нет запросов к HLS дольше этого времени, поток останавливается
- `VIDEO_IDLE_CHECK_PERIOD_MS` (default `30000`) — период проверки

Минимальный API потоков:
- `POST /buildings/{buildingId}/cameras` (роль `MANAGER`) — создать камеру (`rtspUrl`, опционально `transcode=true` для H265/совместимости).
- `GET /buildings/{buildingId}/cameras` — список камер со ссылками `hlsUrl`.
- `GET /buildings/{buildingId}/cameras/{cameraId}/stream` — получить URL и стартовать поток.
- `POST /buildings/{buildingId}/cameras/{cameraId}/stop` (роль `MANAGER`) — остановить поток.