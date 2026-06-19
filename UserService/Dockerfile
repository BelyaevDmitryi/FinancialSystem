FROM eclipse-temurin:17-jre
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
COPY ./target/UserService-0.0.1-SNAPSHOT.jar .
CMD ["java", "-jar", "UserService-0.0.1-SNAPSHOT.jar"]
