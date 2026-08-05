# Stage 1: Build
FROM eclipse-temurin:25-jdk AS builder
WORKDIR /app

COPY gradlew build.gradle settings.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew
RUN ./gradlew dependencies --no-daemon
COPY src ./src
RUN ./gradlew bootJar --no-daemon -x test

# Stage 2: Runtime
FROM eclipse-temurin:25-jre

WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

RUN addgroup -S appuser && \
    adduser -S -G appuser appuser

USER appuser

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:+UseG1GC -XX:TieredStopAtLevel=1 -Dtree-sitter-lib=/tmp"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
