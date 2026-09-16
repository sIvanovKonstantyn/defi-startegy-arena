FROM eclipse-temurin:25-jdk-alpine AS build
WORKDIR /workspace
COPY gradlew settings.gradle build.gradle gradle.properties ./
COPY gradle ./gradle
COPY config ./config
COPY src ./src
COPY scripts ./scripts
RUN ./gradlew installDist --no-daemon

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from=build /workspace/build/install/defi-strategy-arena/ ./
ENV JAVA_TOOL_OPTIONS="-Xms128m -Xmx256m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+ExitOnOutOfMemoryError"
EXPOSE 8080
ENTRYPOINT ["./bin/defi-strategy-arena"]
