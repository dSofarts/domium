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
