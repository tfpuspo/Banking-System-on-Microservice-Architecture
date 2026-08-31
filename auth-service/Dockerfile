# --- Stage 1: build ---
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy only the pom first so Docker can cache dependency downloads
# separately from your actual code changes
COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn package -DskipTests -B

# --- Stage 2: run ---
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

COPY --from=build /app/target/auth-service-1.0.0.jar ./auth-service.jar

EXPOSE 50051
CMD ["java", "-jar", "auth-service.jar"]
