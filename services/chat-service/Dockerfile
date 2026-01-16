FROM gradle:8.14.0-jdk21 AS lib-manager
WORKDIR /app
ENV MAVEN_USER_HOME=/tmp/m2
COPY --chown=gradle:gradle domium-common /app/domium-common
RUN cd domium-common && gradle :security:publishToMavenLocal :openapi:publishToMavenLocal -x test --no-daemon -Dmaven.repo.local=/tmp/m2/repository

FROM gradle:8.14.0-jdk21 AS build
WORKDIR /app
ENV MAVEN_USER_HOME=/tmp/m2
COPY --from=lib-manager /tmp/m2/repository /tmp/m2/repository
COPY --chown=gradle:gradle chat-service /app
RUN gradle :chat-service-main:clean :chat-service-main:build -x test --no-daemon -Dmaven.repo.local=/tmp/m2/repository

FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app
COPY --from=build /app/chat-service-main/build/libs/chat-service.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
