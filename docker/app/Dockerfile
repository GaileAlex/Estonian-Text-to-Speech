FROM eclipse-temurin:25-jdk-noble AS builder
WORKDIR /workspace
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline
COPY src ./src
RUN ./mvnw package -DskipTests

FROM eclipse-temurin:25-jre-noble

RUN groupadd --system app && \
    useradd --system --gid app --no-create-home --shell /usr/sbin/nologin app

WORKDIR /app
COPY --from=builder /workspace/target/*.jar app.jar

USER app

EXPOSE 8380
ENTRYPOINT ["java", "-jar", "app.jar"]
