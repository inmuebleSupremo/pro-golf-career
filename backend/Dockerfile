# Backend — Spring Boot (Java 21). Build context: ./backend
# Multi-stage: build the executable jar with Maven, run it on a slim JRE.

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
# Cache dependencies first for faster rebuilds.
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B -DskipTests package

FROM eclipse-temurin:21-jre
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
# Persisted state (users + saves) lives under /data — mount a volume there.
ENV PROGOLF_USERS_DIR=/data/users \
    PROGOLF_SAVES_DIR=/data/saves
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=5s --retries=12 \
    CMD curl -fsS http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
