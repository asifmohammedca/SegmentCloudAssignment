FROM openjdk:17-jdk-slim

WORKDIR /app

COPY target/OffshoreProxy-0.0.1-SNAPSHOT.jar offshore-proxy.jar

EXPOSE 9090 9091

CMD ["java", "-jar", "offshore-proxy.jar"]
