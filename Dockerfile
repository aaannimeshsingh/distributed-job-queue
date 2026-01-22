# Multi-stage build for minimal image size
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /build

# Copy pom.xml and download dependencies (cached layer)
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime stage - Using regular version for better compatibility
FROM eclipse-temurin:17-jre

WORKDIR /app

# Create non-root user for security
RUN groupadd -r jobqueue && useradd -r -g jobqueue jobqueue

# Copy JAR from builder
COPY --from=builder /build/target/distributed-job-queue-*.jar app.jar

# Change ownership
RUN chown -R jobqueue:jobqueue /app

# Switch to non-root user
USER jobqueue

# Expose health check port (if we add REST API later)
EXPOSE 8080

# Environment variables (can be overridden)
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0" \
    DB_URL="jdbc:postgresql://postgres:5432/jobqueue" \
    DB_USER="jobqueue" \
    DB_PASSWORD="changeme"

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD pgrep -f 'java' || exit 1

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]