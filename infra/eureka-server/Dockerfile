FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /build
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY config-server/pom.xml config-server/pom.xml
COPY eureka-server/pom.xml eureka-server/pom.xml
COPY gateway-server/pom.xml gateway-server/pom.xml
RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline -pl eureka-server -am
COPY eureka-server/src eureka-server/src
RUN ./mvnw clean package -pl eureka-server -am -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring
COPY --from=build /build/eureka-server/target/*.jar app.jar
EXPOSE 8761
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]