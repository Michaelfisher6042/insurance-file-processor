# Multi-stage build for Spring Boot application

# Build stage
FROM openjdk:17-jdk-slim as build

WORKDIR /app

# Copy Gradle wrapper and build files
COPY build.gradle settings.gradle gradlew gradlew.bat ./
COPY gradle/ gradle/

# Copy source code
COPY src/ src/

# Build the application
RUN ./gradlew bootJar --no-daemon

# Runtime stage
FROM openjdk:17-jre-slim

WORKDIR /app

# Copy the built JAR from the build stage
COPY --from=build /app/build/libs/*.jar app.jar

# Expose the default Spring Boot port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
