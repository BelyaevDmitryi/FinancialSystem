FROM openjdk:27-ea-jdk-slim-bookworm AS build

# Установка Maven
WORKDIR /app
#RUN apk update && apk add maven
RUN apt-get update && apt-get install -y maven

# Копирование pom.xml и файлов исходного кода
COPY pom.xml .
COPY src ./src

# Сборка приложения
RUN mvn clean package -DskipTests

# Копирование jar-файла в нужное место
FROM bellsoft/liberica-openjdk-alpine-musl:17
RUN apk add --no-cache curl
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8090
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

