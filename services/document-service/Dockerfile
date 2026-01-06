FROM gradle:8.14.0-jdk21 AS lib-builder
WORKDIR /app
COPY --chown=gradle:gradle domium-common /app/domium-common
RUN cd domium-common && gradle :security:publishToMavenLocal :openapi:publishToMavenLocal -x test --no-daemon

FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /build
COPY --from=lib-builder /root/.m2/repository /root/.m2/repository
COPY document-service/pom.xml ./pom.xml
COPY document-service/src ./src
RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:21-jre
RUN addgroup --system app && adduser --system --ingroup app app
WORKDIR /app
COPY --from=builder /build/target/*.jar app.jar
USER app
EXPOSE 8092
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -Dfile.encoding=UTF-8"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
