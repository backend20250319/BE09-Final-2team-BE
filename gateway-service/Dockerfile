# 1. 빌드 스테이지
FROM gradle:8.7.0-jdk17 AS builder
WORKDIR /app
COPY . .
RUN gradle clean build -x test

# 2. 런타임 스테이지
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar

# 게이트웨이는 8000 고정
EXPOSE 8000

ENTRYPOINT ["java", "-jar", "app.jar"]
