FROM gradle:8.14.0-jdk21 AS lib-manager
WORKDIR /app
COPY --chown=gradle:gradle domium-common /app/domium-common
RUN cd domium-common && gradle :security:publishToMavenLocal -x test --no-daemon

FROM gradle:8.14.0-jdk21 AS build
WORKDIR /app
COPY --from=lib-manager /root/.m2/repository /root/.m2/repository
COPY --chown=gradle:gradle domium-building /app
RUN gradle :app:clean :app:build -x test --no-daemon

FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app
COPY --from=build /app/app/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
