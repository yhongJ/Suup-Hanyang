# 빌드
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

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

# 2GB 서버 기준 메모리 상한
ENV JAVA_OPTS="-Xms256m -Xmx512m"
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
