# --- 1단계: 빌드 ---
FROM gradle:8-jdk17-alpine AS builder
WORKDIR /app
COPY . .
RUN gradle bootJar --no-daemon -x test

# --- 2단계: 실행 ---
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
