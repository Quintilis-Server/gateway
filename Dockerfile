# Multi-stage build for Gateway service
# Stage 1: Build stage
FROM maven:3.9-eclipse-temurin-25-noble AS build

WORKDIR /workspace

# Copy parent pom and common module first for better layer caching
COPY common ./common
COPY gateway/pom.xml ./gateway/
COPY gateway/.mvn ./gateway/.mvn
COPY gateway/mvnw ./gateway/

# Copy gateway source
COPY gateway/src ./gateway/src

# Build the application
WORKDIR /workspace/gateway
RUN mvn clean package -DskipTests


# Stage 2: Runtime stage
FROM eclipse-temurin:25-jre-noble

WORKDIR /app

# Create a non-root user
RUN groupadd -r appuser && useradd -r -g appuser appuser

# Copy the built jar from build stage
COPY --from=build /workspace/gateway/target/*.jar app.jar

# Change ownership
RUN chown -R appuser:appuser /app

# Switch to non-root user
USER appuser

# Expose the application port
EXPOSE 8080

# Set JVM options for container environment
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
