FROM maven:3.8-openjdk-17 as builder

WORKDIR /app
COPY pom.xml .
COPY src ./src

RUN mvn package -DskipTests

FROM openjdk:17-slim

WORKDIR /app
COPY --from=builder /app/target/MeetingSchedulerBot-0.0.1-SNAPSHOT.jar app.jar

CMD ["java", "-jar", "app.jar"]
