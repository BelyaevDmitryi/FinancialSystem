FROM eclipse-temurin:17-jre
COPY ./target/TinkoffStockService-0.0.1-SNAPSHOT.jar .
CMD ["java", "-Dio.netty.noUnsafe=true", "-Dio.netty.noPreferDirect=true", "-Dio.netty.transport.noNative=true", "-jar","TinkoffStockService-0.0.1-SNAPSHOT.jar"]
