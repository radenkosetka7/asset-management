# Stage 1: Build the application
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copy the pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy the source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# Stage 2: Create the runtime image
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create a non-root user
RUN addgroup -S spring && adduser -S spring -G spring

# Copy the built jar from the build stage
COPY --from=build /app/target/*.jar app.jar

# Create assets directory with proper permissions
RUN mkdir -p /app/assets && chown -R spring:spring /app

USER spring

# Expose the application port
EXPOSE 8443

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]

