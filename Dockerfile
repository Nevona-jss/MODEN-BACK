# === Stage 1: Build ===
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copy pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy project source
COPY src ./src

# Build the application
RUN mvn package -DskipTests

# === Stage 2: Run ===
FROM eclipse-temurin:17-jdk
WORKDIR /app

# Copy built jar file from previous stage
COPY --from=build /app/target/*.jar app.jar

# Set environment variables
ENV PORT=8080
EXPOSE 8080

# Start the Spring Boot application
ENTRYPOINT ["java", "-jar", "app.jar"]
