# Stage 1: Build stage
FROM maven:3.8.4-openjdk-17-slim AS build
WORKDIR /app

COPY pom.xml .
COPY common-module/pom.xml common-module/
COPY cache-service/pom.xml cache-service/

RUN mkdir -p auth-service && \
    echo '<project xmlns="http://maven.apache.org/POM/4.0.0"><modelVersion>4.0.0</modelVersion><parent><groupId>dev.batuhanyetgin</groupId><artifactId>dt-parent</artifactId><version>0.0.1</version></parent><artifactId>auth-service</artifactId></project>' > auth-service/pom.xml && \
    mkdir -p debt-service && \
    echo '<project xmlns="http://maven.apache.org/POM/4.0.0"><modelVersion>4.0.0</modelVersion><parent><groupId>dev.batuhanyetgin</groupId><artifactId>dt-parent</artifactId><version>0.0.1</version></parent><artifactId>debt-service</artifactId></project>' > debt-service/pom.xml && \
    mkdir -p notification-service && \
    echo '<project xmlns="http://maven.apache.org/POM/4.0.0"><modelVersion>4.0.0</modelVersion><parent><groupId>dev.batuhanyetgin</groupId><artifactId>dt-parent</artifactId><version>0.0.1</version></parent><artifactId>notification-service</artifactId></project>' > notification-service/pom.xml

RUN mvn dependency:go-offline -B -pl common-module,cache-service -am

COPY common-module/src ./common-module/src
COPY cache-service/src ./cache-service/src

RUN mvn clean install -DskipTests -pl common-module -am
RUN mvn clean package -DskipTests -pl cache-service

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

COPY --from=build /app/cache-service/target/*.jar app.jar

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
