FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /build
COPY pom.xml .
COPY config-server/pom.xml config-server/pom.xml
COPY eureka-server/pom.xml eureka-server/pom.xml
COPY gateway-server/pom.xml gateway-server/pom.xml
RUN mvn dependency:go-offline -pl config-server -am
COPY config-server/src config-server/src
RUN mvn clean package -pl config-server -am -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring
COPY --from=build /build/config-server/target/*.jar app.jar
EXPOSE 8888
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]