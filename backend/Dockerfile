# ==============================================================================
# SwiftRouteOS Backend - Production Multi-Stage Dockerfile
# Stage 1: Build & Layer Extraction using Eclipse Temurin JDK 21
# Stage 2: Hardened, Minimal JRE Runtime with Non-Root Security Context
# ==============================================================================

# ------------------------------------------------------------------------------
# Stage 1: Builder
# ------------------------------------------------------------------------------
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# 1. Dependency Resolution Cache Layer
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 2. Source Compilation & Packaging Layer
COPY src ./src
RUN mvn clean package -DskipTests -B

# 3. Layered Spring Boot 3.4 Tools Extraction Layer
# Unpacks external dependencies into ./lib and creates thin application JAR
RUN java -Djarmode=tools -jar target/swiftroute-backend-*.jar extract --destination target/extracted

# ------------------------------------------------------------------------------
# Stage 2: Minimal Production JRE Runtime
# ------------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine AS runtime

LABEL maintainer="SwiftRouteOS Engineering <team@swiftroute.io>" \
      description="SwiftRouteOS Intelligent SLA-Aware Field Service Dispatch Backend" \
      version="1.0.0"

# Install curl and ca-certificates for robust container health checks
RUN apk --no-cache add curl ca-certificates && rm -rf /var/cache/apk/*

WORKDIR /app

# Create dedicated non-root application user and group (Least Privilege Principle)
RUN addgroup -S swiftroute && adduser -S swiftroute -G swiftroute

# Copy extracted libraries and application jar from builder
COPY --from=builder --chown=swiftroute:swiftroute /build/target/extracted/lib ./lib
COPY --from=builder --chown=swiftroute:swiftroute /build/target/extracted/*.jar ./app.jar

# Run container as non-root user
USER swiftroute:swiftroute

# Optimized Container JVM Ergonomics
ENV JAVA_TOOL_OPTIONS="-XX:+UseG1GC -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom"
ENV SPRING_PROFILES_ACTIVE=prod
ENV SERVER_PORT=8080

EXPOSE 8080

# Production Health Check against Spring Boot Actuator
HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
