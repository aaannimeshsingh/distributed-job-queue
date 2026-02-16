# Build stage
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app

# Copy pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source and build
COPY src ./src
RUN mvn clean package -DskipTests

# Run stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copy the JAR
COPY --from=builder /app/target/job-queue-api.jar app.jar

# Expose port
EXPOSE 8080

# Run API
CMD ["java", "-jar", "app.jar"]
