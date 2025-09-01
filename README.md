# DART 공시 수집 및 협력사 재무 리스크 분석 시스템

기업의 공시정보를 자동으로 수집하고, 협력사의 재무 데이터를 기반으로 리스크를 분석하여 ESG 평가 및 대응을 지원하는 백엔드 서비스입니다.  
Kafka 기반 비동기 수신 구조와 외부 OpenAPI 연동을 통해 실시간 분석이 가능합니다.

---

## 주요 기능

| 기능 영역 | 설명 |
|-----------|------|
| 기업 정보 검색 | 기업명 또는 고유 코드 기반으로 DART 기업 상세정보를 조회 |
| 협력사 등록 | 사용자 요청 기반 협력사 등록 및 Kafka 전송 처리 |
| 공시정보 수신 | Kafka consumer가 DART 공시 수신 후 DB 저장 |
| 재무제표 수집 | XBRL 형식의 DART 재무제표를 JSON 또는 Taxonomy로 변환 후 저장 |
| 재무위험 분석 | 12개 재무항목 기반 협력사 위험 분석 수행 |
| 협력사 조회 | 협력사 목록 또는 상세 정보 반환 API 제공 |

---

##  기술 스택

- **Spring Boot** + Spring Web
- **Kafka** (생산자/소비자 구조)
- **WebClient** (외부 DART OpenAPI 통신)
- **MySQL** (기업 정보 및 공시 저장소)
- **XBRL 파싱** (ZIP → XML → JSON or Taxonomy)
- **Docker**, **Kubernetes**, **EKS (선택 사항)**

---

## 핵심 클래스 흐름 요약

### 1. 기업 정보 검색
- `DartCompanyController.searchByKeyword()`  
  → `DartCompanyService.searchByKeyword()`

### 2. 기업 상세 조회
- `DartCompanyController.getDetails()`  
  → `DartCompanyService.getCorpDetails()`

### 3. 협력사 등록
- `PartnerCompanyController.register()`  
  → `PartnerCompanyService.register()`  
  → `KafkaProducerService.send()`

### 4. Kafka 수신 및 공시정보 수집
- `KafkaConsumerService.receive()`  
  → `DartOpenApiDisclosureService.getDisclosures()`  
  → 저장: `disclosure`

### 5. 재무제표 수집 및 저장
- `DartOpenApiFinancialStatementService.getFinancialStatements()`  
  → `WebClientService.getFinancialStatementApi()`  
    - XBRL ZIP 저장 → XML → JSON 파싱 → 저장: `raw_xbrl_data`  
    - XML → Taxonomy → 저장: `financial_statement_data`

### 6. 재무위험 분석
- `PartnerFinancialRiskService.assessFinancialRisk()`  
  → DB 조회: `financial_statement_data`  
  → 12가지 항목 분석 → DTO 반환

### 7. 협력사 목록 및 상세조회
- `PartnerCompanyController.getListOrDetail()`  
  → `PartnerCompanyService.getListOrDetail()`

---
