# syntax=docker/dockerfile:1.6

FROM gradle:8.10-jdk21 AS builder
WORKDIR /workspace
COPY gradlew ./
COPY gradle ./gradle
COPY settings.gradle build.gradle ./
COPY src ./src
RUN chmod +x gradlew \
    && ./gradlew clean bootJar -x test

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS=""
COPY --from=builder /workspace/build/libs/*.jar app.jar
EXPOSE 8080
EXPOSE 5684/udp
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]
