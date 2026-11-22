# Stage 1: Build stage
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml .
COPY common-module/pom.xml common-module/
COPY cache-service/pom.xml cache-service/

RUN mkdir -p auth-service && \
    echo '<project xmlns="http://maven.apache.org/POM/4.0.0"><modelVersion>4.0.0</modelVersion><parent><groupId>dev.batuhanyetgin</groupId><artifactId>kamci-parent</artifactId><version>0.0.1</version></parent><artifactId>auth-service</artifactId></project>' > auth-service/pom.xml && \
    mkdir -p debt-service && \
    echo '<project xmlns="http://maven.apache.org/POM/4.0.0"><modelVersion>4.0.0</modelVersion><parent><groupId>dev.batuhanyetgin</groupId><artifactId>kamci-parent</artifactId><version>0.0.1</version></parent><artifactId>debt-service</artifactId></project>' > debt-service/pom.xml && \
    mkdir -p notification-service && \
    echo '<project xmlns="http://maven.apache.org/POM/4.0.0"><modelVersion>4.0.0</modelVersion><parent><groupId>dev.batuhanyetgin</groupId><artifactId>kamci-parent</artifactId><version>0.0.1</version></parent><artifactId>notification-service</artifactId></project>' > notification-service/pom.xml

RUN mvn dependency:go-offline -B -pl common-module,cache-service -am

COPY common-module/src ./common-module/src
COPY cache-service/src ./cache-service/src

RUN mvn clean install -DskipTests -pl common-module -am
RUN mvn clean package -DskipTests -pl cache-service

FROM eclipse-temurin:24-jre-alpine
WORKDIR /app
RUN addgroup -S kamci && adduser -S -G kamci kamci

COPY --from=build /app/cache-service/target/*.jar app.jar

USER kamci

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
