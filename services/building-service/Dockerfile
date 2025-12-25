FROM gradle:8.14.0-jdk21 AS lib-builder
WORKDIR /app
COPY --chown=gradle:gradle domium-common /app/domium-common
RUN cd domium-common && gradle :security:publishToMavenLocal -x test --no-daemon

FROM gradle:8.14.0-jdk21 AS builder
WORKDIR /app
COPY --from=lib-builder /root/.m2/repository /root/.m2/repository
COPY --chown=gradle:gradle building-service /app
RUN gradle clean build -x test --no-daemon

FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
