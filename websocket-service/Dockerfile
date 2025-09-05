# 1. 빌드 스테이지
FROM gradle:8.7.0-jdk17 AS builder
WORKDIR /app
COPY . .
RUN gradle clean build -x test

# 2. 런타임 스테이지
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar

# WebSocket 서비스 포트는 8085
EXPOSE 8085

ENTRYPOINT ["java", "-jar", "app.jar"]
