# Build dependencies separately so source changes can reuse the Maven cache.
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /workspace
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw --batch-mode --no-transfer-progress -Dmaven.test.skip=true dependency:go-offline
COPY src/main/ src/main/
# Integration tests require PostgreSQL and should run in CI before deployment.
RUN ./mvnw --batch-mode --no-transfer-progress -Dmaven.test.skip=true clean package

FROM eclipse-temurin:21-jre-jammy AS runtime
WORKDIR /app
RUN groupadd --system app && useradd --system --gid app --home-dir /app --no-create-home app
COPY --from=build /workspace/target/*.jar /app/app.jar
ENV SPRING_PROFILES_ACTIVE=prod
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
