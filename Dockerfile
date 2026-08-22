# ---- Build stage -----------------------------------------------------
# Compiles the app with Maven + a JDK. This image is discarded after the
# build; only the jar it produces makes it into the final image.
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy the pom first and download dependencies into a cached layer, so a
# code-only change doesn't re-download the whole dependency tree.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ---- Runtime stage -----------------------------------------------------
# Slim JRE-only image -- no Maven, no JDK, just what's needed to run the jar.
# postgresql-client is added because BackupService shells out to pg_dump /
# psql for the backup & restore feature; without it those endpoints fail
# at runtime (the app itself still starts fine).
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends postgresql-client \
    && rm -rf /var/lib/apt/lists/*

# Render/most PaaS containers run as root by default; drop to a
# non-root user for a slightly safer runtime.
RUN useradd --system --create-home appuser
USER appuser

COPY --from=build /app/target/*.jar app.jar

# Render sets $PORT at runtime; application.yml already reads it
# (server.port: ${PORT:8080}), so no extra wiring is needed here.
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
