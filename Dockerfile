# Этап 1: сборка JAR-файла
FROM eclipse-temurin:17-jdk-jammy as builder
WORKDIR /workspace
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline
COPY src ./src
COPY process_tts.py /app/
RUN ./mvnw package -DskipTests

# Этап 2: финальный образ (с Python)
FROM eclipse-temurin:17-jre-jammy

RUN apt-get update && \
    apt-get install -y python3 python3-pip && \
    apt-get clean && rm -rf /var/lib/apt/lists/* && \
    ln -s /usr/bin/python3 /usr/bin/python

RUN python3 -m pip install --upgrade pip

WORKDIR /app
COPY --from=builder /workspace/target/*.jar app.jar
COPY process_tts.py /app/

EXPOSE 8380
ENTRYPOINT ["java", "-jar", "app.jar"]
