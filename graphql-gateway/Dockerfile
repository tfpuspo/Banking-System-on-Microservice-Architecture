# --- Stage 1: build ---
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn package -DskipTests -B

# --- Stage 2: run ---
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

COPY --from=build /app/target/graphql-gateway-1.0.0.jar ./graphql-gateway.jar

EXPOSE 4000
CMD ["java", "-jar", "graphql-gateway.jar"]
