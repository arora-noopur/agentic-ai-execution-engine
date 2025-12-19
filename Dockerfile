# --- Stage 1: Build the JAR ---
FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /app

# Copy configuration first to leverage Docker cache for dependencies
COPY pom.xml .
# Download dependencies (this step is cached if pom.xml doesn't change)
RUN mvn dependency:go-offline

# Copy the source code and build
COPY src ./src
RUN mvn clean package -DskipTests

# --- Stage 2: Create the Runtime Image ---
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create a non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy the JAR from the builder stage
COPY --from=builder /app/target/*.jar app.jar

# Expose the port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]