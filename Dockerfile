# 🔧 빌드 스테이지
FROM amazoncorretto:17-alpine AS build
WORKDIR /workspace/app

COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# 의존성 캐싱
RUN chmod +x ./gradlew
RUN ./gradlew dependencies --no-daemon

COPY src src
RUN ./gradlew clean bootJar -x test --no-daemon
RUN ls -la build/libs/

# 🚀 실행 스테이지
FROM amazoncorretto:17-alpine
WORKDIR /app

RUN apk add --no-cache tzdata mysql-client
ENV TZ=Asia/Seoul

# JAR 복사 및 명확한 이름 지정
COPY --from=build /workspace/app/build/libs/*.jar app.jar

# 실행
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
