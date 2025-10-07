FROM bellsoft/liberica-openjdk-alpine-musl:17
COPY ./target/TinkoffStockService-0.0.1-SNAPSHOT.jar .
CMD ["java", "--enable-native-access=ALL-UNNAMED", "-jar","TinkoffStockService-0.0.1-SNAPSHOT.jar"]
