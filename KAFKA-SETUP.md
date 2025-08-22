# DART Service 카프카 환경 설정 가이드

## 개요

DART Service는 다음과 같은 카프카 토픽을 사용하여 마이크로서비스 간 통신을 수행합니다:

- `dart-company-profile`: 회사 프로필 정보
- `dart-disclosure`: 공시 정보
- `dart-corp-code`: 기업 코드 정보
- `partner-company-updated`: 파트너 회사 업데이트
- `partner-company-restore`: 파트너 회사 복원

## 카프카 환경 시작하기

### 1. Docker Compose로 카프카 클러스터 시작

```bash
# dart-service 디렉토리에서 실행
cd /path/to/dart-service
docker-compose up -d
```

### 2. 서비스 상태 확인

```bash
# 실행 중인 컨테이너 확인
docker-compose ps

# 카프카 로그 확인
docker-compose logs kafka

# 모든 서비스 로그 확인
docker-compose logs -f
```

### 3. 토픽 생성 확인

토픽은 `kafka-init` 서비스에 의해 자동으로 생성됩니다. 수동으로 확인하려면:

```bash
# 카프카 컨테이너에 접속
docker exec -it dart-kafka bash

# 토픽 목록 확인
kafka-topics --list --bootstrap-server localhost:9092

# 특정 토픽 상세 정보 확인
kafka-topics --describe --topic partner-company-updated --bootstrap-server localhost:9092
```

## 카프카 UI 접근

웹 브라우저에서 `http://localhost:8090`으로 접속하여 카프카 클러스터를 관리할 수 있습니다.

- 토픽 생성/삭제
- 메시지 조회
- 컨슈머 그룹 모니터링
- 클러스터 상태 확인

## DART Service 설정

### application-dev.yml 설정

```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9093 # 외부 접근용 포트
    consumer:
      group-id: dart-api-group
      auto-offset-reset: earliest
```

### 환경변수 설정

```bash
# 카프카 브로커 주소
export KAFKA_BOOTSTRAP_SERVERS=localhost:9093
```

## 포트 정보

| 서비스       | 포트 | 설명                   |
| ------------ | ---- | ---------------------- |
| Zookeeper    | 2181 | 클러스터 코디네이션    |
| Kafka (내부) | 9092 | 컨테이너 간 통신       |
| Kafka (외부) | 9093 | 외부 애플리케이션 접근 |
| Kafka UI     | 8090 | 웹 관리 인터페이스     |

## 메시지 테스트

### 메시지 생산 (Producer) 테스트

```bash
# 카프카 컨테이너에서 메시지 전송
docker exec -it dart-kafka kafka-console-producer \
  --bootstrap-server localhost:9092 \
  --topic partner-company-updated

# 메시지 입력 후 Enter
{"id": 1, "corpName": "테스트회사", "corpCode": "00000001"}
```

### 메시지 소비 (Consumer) 테스트

```bash
# 카프카 컨테이너에서 메시지 수신
docker exec -it dart-kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic partner-company-updated \
  --from-beginning
```

## 문제 해결

### 1. 포트 충돌 시

`docker-compose.yml`에서 포트를 변경하세요:

```yaml
ports:
  - "9094:9093" # 외부 포트 변경
```

### 2. 메모리 부족 시

카프카 힙 메모리를 줄이세요:

```yaml
environment:
  KAFKA_HEAP_OPTS: "-Xmx512M -Xms512M"
```

### 3. 토픽 자동 생성 비활성화

프로덕션 환경에서는 자동 생성을 비활성화하는 것을 권장합니다:

```yaml
environment:
  KAFKA_AUTO_CREATE_TOPICS_ENABLE: "false"
```

## 종료 및 정리

```bash
# 카프카 클러스터 중지
docker-compose down

# 데이터까지 모두 삭제
docker-compose down -v

# 이미지까지 모두 삭제
docker-compose down -v --rmi all
```
