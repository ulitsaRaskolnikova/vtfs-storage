FROM gradle:8.5-jdk17 AS build

WORKDIR /app

# Копируем файлы сборки
COPY build.gradle settings.gradle gradle.properties ./
COPY gradle ./gradle

# Копируем исходный код
COPY src ./src

# Собираем приложение
RUN gradle build -x test --no-daemon

# Финальный образ
FROM eclipse-temurin:17-jre

WORKDIR /app

# Копируем собранный JAR
COPY --from=build /app/build/libs/*.jar app.jar

# Открываем порт
EXPOSE 8080

# Запускаем приложение
ENTRYPOINT ["java", "-jar", "app.jar"]
