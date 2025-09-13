# Stage 1: Build the application using Maven
FROM maven:3.8-openjdk-21 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# Stage 2: Create the final, smaller image with only the necessary files
FROM openjdk:17-slim
WORKDIR /app
COPY --from=build /app/target/chromiq-1.0-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]