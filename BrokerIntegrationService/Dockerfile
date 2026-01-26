FROM eclipse-temurin:17-jre
COPY ./target/BrokerIntegrationService-0.0.1-SNAPSHOT.jar .
CMD ["java", "-Dio.netty.noUnsafe=true", "-Dio.netty.noPreferDirect=true", "-Dio.netty.transport.noNative=true", "-jar","BrokerIntegrationService-0.0.1-SNAPSHOT.jar"]
