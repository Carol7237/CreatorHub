# syntax=docker/dockerfile:1
# Single, parameterized multi-stage Dockerfile for every CreatorHub service.
# Build context = ./services (the whole multi-module reactor), so the build can
# resolve the shared `common` module. Pick the module with the MODULE build-arg.
#
#   docker build --build-arg MODULE=user-service -f services/Dockerfile ./services
#
# `mvn -pl ${MODULE} -am` (also-make) builds `common` first when a service needs it.

# ---- build stage: full JDK + Maven, builds the fat jar ----
FROM maven:3.9-eclipse-temurin-21 AS build
ARG MODULE
WORKDIR /workspace
COPY . .
# Cache the local Maven repo across builds (BuildKit) so we don't re-download every time.
RUN --mount=type=cache,target=/root/.m2 \
    mvn -q -pl ${MODULE} -am -DskipTests clean package

# ---- runtime stage: JRE only, small image ----
FROM eclipse-temurin:21-jre AS runtime
ARG MODULE
# curl is used by the Docker Compose healthcheck (hits /actuator/health).
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /workspace/${MODULE}/target/${MODULE}-0.0.1-SNAPSHOT.jar /app/app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
