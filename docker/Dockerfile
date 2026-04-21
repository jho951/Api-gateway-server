# syntax=docker/dockerfile:1.7

FROM gradle:8.14.3-jdk17 AS builder

WORKDIR /workspace

COPY gradle gradle
COPY gradlew gradlew
COPY build.gradle settings.gradle gradle.properties ./
COPY app/build.gradle app/build.gradle
COPY common/build.gradle common/build.gradle

RUN chmod +x gradlew

# Resolve the multi-module build graph first so Docker layer caching survives most source-only changes.
RUN --mount=type=secret,id=gh_token,required=false \
    --mount=type=secret,id=github_actor,required=false \
    if [ -f /run/secrets/gh_token ]; then export GH_TOKEN="$(cat /run/secrets/gh_token)"; fi; \
    if [ -f /run/secrets/github_actor ]; then export GITHUB_ACTOR="$(cat /run/secrets/github_actor)"; fi; \
    ./gradlew --no-daemon :app:dependencies >/dev/null

COPY app app
COPY common common

RUN --mount=type=secret,id=gh_token,required=false \
    --mount=type=secret,id=github_actor,required=false \
    if [ -f /run/secrets/gh_token ]; then export GH_TOKEN="$(cat /run/secrets/gh_token)"; fi; \
    if [ -f /run/secrets/github_actor ]; then export GITHUB_ACTOR="$(cat /run/secrets/github_actor)"; fi; \
    ./gradlew --no-daemon clean :app:bootJar

FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

COPY --from=builder /workspace/app/build/libs/*.jar /app/gateway-service.jar
RUN touch /app/.env.dev /app/.env.prod

EXPOSE 8080

ENV GATEWAY_BIND=0.0.0.0
ENV GATEWAY_PORT=8080

CMD ["java", "-jar", "/app/gateway-service.jar"]
