FROM openjdk:17-jdk-slim

WORKDIR /app

COPY target/ShipProxy-0.0.1-SNAPSHOT.jar ship-proxy.jar

EXPOSE 8080

CMD ["java", "-jar", "ship-proxy.jar"]
