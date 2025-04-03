FROM maven:3.8.6-openjdk-11-slim AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src /app/src

# In build stage, ensure resources are copied
COPY src/main/resources/ /app/src/main/resources/

# Build with explicit main class reference
RUN mvn package -DskipTests

FROM openjdk:11-jre-slim
WORKDIR /app
COPY --from=build /app/target/app.jar .

# WebSocket server typically needs these ports
EXPOSE 8080 8443

# Add server configuration options
ENTRYPOINT ["java", "-jar", "app.jar"]