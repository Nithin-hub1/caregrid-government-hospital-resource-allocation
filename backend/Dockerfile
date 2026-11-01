# Multi-stage Docker build for CareGrid Government Hospital Resource Allocation Backend
# Stage 1: Build JAR using Maven and Eclipse Temurin JDK 25
FROM maven:3-eclipse-temurin-25 AS build
WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Runtime container with lightweight Eclipse Temurin 25 JRE
FROM eclipse-temurin:25-jre
WORKDIR /app

COPY --from=build /app/target/resource-allocation-backend-*.jar app.jar

ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
