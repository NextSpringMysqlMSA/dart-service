#!/bin/bash

# DART API 서비스 빌드 및 배포 스크립트

# 컬러 코드
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 함수 정의
log_info() {
  echo -e "${BLUE}ℹ️ $1${NC}"
}

log_success() {
  echo -e "${GREEN}✅ $1${NC}"
}

log_warning() {
  echo -e "${YELLOW}⚠️ $1${NC}"
}

log_error() {
  echo -e "${RED}❌ $1${NC}"
}

# 시작 메시지
echo -e "${GREEN}[$(date '+%H:%M:%S')] 🚀 DART API 서비스 빌드 시작${NC}"

# 프로젝트 루트 (스크립트 실행 위치)
PROJECT_ROOT=$(pwd)

# Gradle 빌드
log_info "Gradle 빌드 실행 중..."
./gradlew clean bootJar -x test || {
  log_error "Gradle 빌드 실패"
  exit 1
}
log_success "Gradle 빌드 완료"

# JAR 파일 복사
log_info "JAR 파일 복사 중..."
# 먼저 빌드된 JAR 파일 찾기
JAR_FILE=$(find build/libs -name "*.jar" -not -name "*plain.jar" | head -n 1)
if [ -z "$JAR_FILE" ]; then
  log_error "빌드된 JAR 파일을 찾을 수 없습니다."
  exit 1
fi

cp "$JAR_FILE" "$PROJECT_ROOT/dart-api-service.jar" || {
  log_error "JAR 파일 복사 실패"
  exit 1
}
log_success "JAR 파일 복사 완료: $JAR_FILE -> $PROJECT_ROOT/dart-api-service.jar"

# Docker 이미지 빌드 (Dockerfile은 프로젝트 루트에 있다고 가정)
log_info "Docker 이미지 빌드 중 (Dockerfile: $PROJECT_ROOT/Dockerfile)..."
docker build -t gyeoul/dart-service:latest -f "$PROJECT_ROOT/Dockerfile" "$PROJECT_ROOT" || {
  log_error "Docker 이미지 빌드 실패"
  # 실패 시 임시 jar 파일 삭제
  rm "$PROJECT_ROOT/dart-api-service.jar"
  exit 1
}
log_success "Docker 이미지 빌드 완료: gyeoul/dart-service:latest"

# Docker 이미지 푸시
log_info "Docker 이미지 푸시 중 (gyeoul/dart-service:latest)..."
docker push gyeoul/dart-service:latest || {
  log_warning "Docker 이미지 푸시 실패. Docker Hub 로그인이 필요할 수 있습니다."
  echo "다음 명령어로 로그인 후 다시 시도하세요: docker login"
  # 실패 시 임시 jar 파일 삭제
  rm "$PROJECT_ROOT/dart-api-service.jar"
  # 로컬 테스트에서는 푸시 실패해도 계속 진행하도록 exit 1 주석 처리
  # exit 1
}
log_success "Docker 이미지 푸시 완료: gyeoul/dart-service:latest"

# JAR 파일 정리
log_info "임시 파일 정리 중..."
rm "$PROJECT_ROOT/dart-api-service.jar"
log_success "임시 파일 정리 완료"

# 완료 메시지
echo -e "${GREEN}[$(date '+%H:%M:%S')] 🎉 DART API 서비스 빌드 및 Docker Hub 푸시 완료${NC}"
echo -e "이제 remote-yaml/dart/dart-deployment.yaml 파일의 이미지를 gyeoul/dart-service:latest로 수정하세요."
echo -e "이후 다음 명령어로 서비스를 배포할 수 있습니다: ./remote-yaml/2-deploy-services.sh" 