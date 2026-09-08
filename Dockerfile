FROM maven:3.9.11-eclipse-temurin-17 AS build

ARG MAVEN_OPTS

WORKDIR /workspace

COPY pom.xml ./
RUN mvn -B -ntp dependency:go-offline

COPY src ./src
RUN mvn -B -ntp -DskipTests package

FROM eclipse-temurin:17-jre AS runtime

ENV TZ=Asia/Shanghai

RUN groupadd --system spring && useradd --system --gid spring --create-home spring
WORKDIR /app

COPY --from=build --chown=spring:spring \
    /workspace/target/local-review-platform-1.0.0.jar \
    /app/application.jar

USER spring:spring
EXPOSE 8080

ENTRYPOINT ["java", "-Duser.timezone=Asia/Shanghai", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/application.jar"]
