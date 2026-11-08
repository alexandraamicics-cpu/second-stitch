# Builds and runs the Spring Boot API in a container (used by Render and other hosts).

# --- Stage 1: build the .jar with Maven ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
# Download dependencies first so they're cached between builds
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests

# --- Stage 2: run it on a small Java runtime image ---
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
# Free hosting plans have little memory; let Java use most of what's there
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
