FROM maven:3.9.13-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
COPY config-server/pom.xml config-server/pom.xml
COPY eureka-server/pom.xml eureka-server/pom.xml
COPY gateway-server/pom.xml gateway-server/pom.xml
RUN mvn dependency:go-offline -pl config-server -am
COPY config-server/src config-server/src
RUN mvn clean package -pl config-server -am -DskipTests

FROM eclipse-temurin:22-jre-jammy
WORKDIR /app
RUN groupadd -r spring && useradd -r -g spring spring
USER spring:spring
COPY --from=build /build/config-server/target/*.jar app.jar
EXPOSE 8888
HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=5 CMD wget -q --spider http://localhost:8888/actuator/health || exit 1
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]