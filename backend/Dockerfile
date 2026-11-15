# syntax=docker/dockerfile:1

# Build stage. Tests are not run here: they need Docker themselves (Testcontainers) and run with `mvn test`.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -B -q dependency:go-offline
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -q package -DskipTests

# Runtime stage: JRE only, non-root user.
FROM eclipse-temurin:21-jre
RUN useradd --system --no-create-home claims
WORKDIR /app
COPY --from=build /workspace/target/claims-service-*.jar app.jar
USER claims
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
