FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn package -DskipTests -B

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/ledger-service-1.0.0.jar ./ledger-service.jar
EXPOSE 50051
CMD ["java", "-jar", "ledger-service.jar"]
