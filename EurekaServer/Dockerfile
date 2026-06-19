FROM openjdk:27-ea-jdk-slim-bookworm AS build

WORKDIR /app
# Установка Maven
#RUN apk update && apk add maven
RUN apt-get update && apt-get install -y maven
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests
# Диагностика: покажем содержимое target/
RUN echo "=== Contents of /app/target/ ===" && ls -la /app/target/

FROM bellsoft/liberica-openjdk-alpine-musl:17
RUN apk add --no-cache curl
WORKDIR /app
#RUN ls -la /app/target/
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8761
ENTRYPOINT ["java", "-jar", "app.jar"]
