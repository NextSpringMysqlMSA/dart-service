FROM amazoncorretto:17-alpine

WORKDIR /app

COPY dart-api-service.jar app.jar

EXPOSE 8081

ENV TZ=Asia/Seoul

ENTRYPOINT ["java", "-jar", "/app/app.jar"] 