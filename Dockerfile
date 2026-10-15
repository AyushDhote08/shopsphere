# ==============================================================================
# ShopSphere Multi-Stage Production Dockerfile
# ==============================================================================
# Stage 1: Dependency Caching & Compilation (Maven + Eclipse Temurin JDK 21)
# ==============================================================================
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
WORKDIR /workspace

# Layer 1: Cache dependencies by copying only pom.xml first
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Layer 2: Copy source code and build executable JAR
COPY src src
RUN mvn clean package -DskipTests=true -B

# ==============================================================================
# Stage 2: Hardened Lightweight Runtime (Eclipse Temurin JRE 21 Alpine ~140MB)
# ==============================================================================
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Security: Create non-root system group and user
RUN addgroup -S shopsphere && adduser -S shopsphere -G shopsphere

# Copy compiled jar from builder stage with appropriate permissions
COPY --from=builder --chown=shopsphere:shopsphere /workspace/target/shopsphere-*.jar app.jar

# Run as unprivileged user
USER shopsphere:shopsphere

# Default port (can be overridden dynamically via PORT environment variable)
EXPOSE 8080

# Production JVM ergonomics: constrained memory footprint & fast G1 garbage collector
ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -XX:+ExitOnOutOfMemoryError"

# Launch application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
