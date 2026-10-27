# 빌드
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# 1GB 서버 빌드용 메모리 제한
ENV GRADLE_OPTS="-Xmx384m -Dorg.gradle.jvmargs=-Xmx384m"

# 의존성 캐시
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies > /dev/null

COPY src src
RUN ./gradlew --no-daemon bootJar -x test

# 실행
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar

# 1GB 서버 기준 메모리 상한
ENV JAVA_OPTS="-Xms128m -Xmx320m -XX:MaxMetaspaceSize=160m -XX:+UseSerialGC"
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]