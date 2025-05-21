#!/bin/bash

# 프로젝트 빌드 (테스트 건너뛰기)
echo "프로젝트 빌드 중..."
./gradlew clean build -x test

# 빌드 성공 여부 확인
if [ $? -eq 0 ]; then
    echo "빌드 성공! 애플리케이션을 실행합니다."

    # JAR 파일 실행
    java -jar build/libs/dart-api-service.jar -D
else
    echo "빌드 실패. 코드를 확인해주세요."
    exit 1
fi 
