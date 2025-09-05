FROM gradle:8.7.0-jdk17 AS builder
WORKDIR /app
COPY . .
RUN gradle clean build -x test

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar

# Eureka 서버는 항상 8761 포트 고정
EXPOSE 8761

ENTRYPOINT ["java", "-jar", "app.jar"]
