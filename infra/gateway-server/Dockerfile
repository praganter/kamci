FROM maven:3.9.13-eclipse-temurin-17-alpine AS build
WORKDIR /build
COPY pom.xml .
COPY config-server/pom.xml config-server/pom.xml
COPY eureka-server/pom.xml eureka-server/pom.xml
COPY gateway-server/pom.xml gateway-server/pom.xml
RUN mvn dependency:go-offline -pl gateway-server -am
COPY gateway-server/src gateway-server/src
RUN mvn clean package -pl gateway-server -am -DskipTests

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
RUN groupadd -r spring && useradd -r -g spring spring
USER spring:spring
COPY --from=build /build/gateway-server/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]