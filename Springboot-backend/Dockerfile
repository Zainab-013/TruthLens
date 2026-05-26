# Stage 1: Build the Maven application
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Create a lightweight runtime image
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /app/target/truthlens-backend-1.0.0.jar app.jar

# Expose port
EXPOSE 8080

# Run JVM with environment variables mapped to properties
ENTRYPOINT ["java", "-Dnlp.api.url=${NLP_API_URL}", "-jar", "app.jar"]
