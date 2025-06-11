package com.example.javaversion.partner.service;

import com.example.javaversion.database.entity.PartnerCompany;
import com.example.javaversion.database.repository.PartnerCompanyRepository;
import com.example.javaversion.kafka.dto.NewsAnalysisRequest;
import com.example.javaversion.kafka.service.KafkaProducerService;
import com.example.javaversion.partner.model.PartnerCompanyStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 파트너사 뉴스 크롤링 스케줄러 서비스
 * 주기적으로 ACTIVE 상태인 파트너사들의 뉴스를 크롤링 요청합니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PartnerNewsSchedulerService {

    private final PartnerCompanyRepository partnerCompanyRepository;
    private final KafkaProducerService kafkaProducerService;

    @Value("${kafka.topic.news-keywords}")
    private String newsKeywordsTopic;

    @Value("${partner.news.scheduler.enabled:true}")
    private boolean schedulerEnabled;

    /**
     * 매일 오전 9시에 파트너사 뉴스 크롤링 실행
     * 크론 표현식: 초 분 시 일 월 요일
     */
    @Scheduled(cron = "0 0 9 * * *")
    public void scheduleDailyNewsAnalysis() {
        if (!schedulerEnabled) {
            log.info("파트너사 뉴스 스케줄러가 비활성화되어 있습니다.");
            return;
        }

        log.info("=== 파트너사 일일 뉴스 크롤링 스케줄 시작 ===");
        executeNewsAnalysis("1d", "1w"); // 최근 1일, 1주일
    }

    /**
     * 매주 월요일 오전 8시에 주간 뉴스 크롤링 실행 (더 긴 기간)
     */
    @Scheduled(cron = "0 0 8 * * MON")
    public void scheduleWeeklyNewsAnalysis() {
        if (!schedulerEnabled) {
            log.info("파트너사 뉴스 스케줄러가 비활성화되어 있습니다.");
            return;
        }

        log.info("=== 파트너사 주간 뉴스 크롤링 스케줄 시작 ===");
        executeNewsAnalysis("1w", "1m"); // 최근 1주일, 1개월
    }

    /**
     * 개발/테스트용: 매 10분마다 실행 (프로덕션에서는 비활성화)
     * 환경변수 PARTNER_NEWS_TEST_SCHEDULE_ENABLED=true로 활성화
     */
    @Scheduled(cron = "0 */10 * * * *")
    public void scheduleTestNewsAnalysis() {
        boolean testScheduleEnabled = Boolean.parseBoolean(
                System.getProperty("partner.news.test.schedule.enabled", "false")
        );
        
        if (!schedulerEnabled || !testScheduleEnabled) {
            return; // 로그 없이 조용히 건너뜀
        }

        log.info("=== 파트너사 테스트 뉴스 크롤링 스케줄 시작 (10분 주기) ===");
        executeNewsAnalysis("1d"); // 최근 1일만
    }

    /**
     * 실제 뉴스 분석 실행 로직 (공통)
     */
    private void executeNewsAnalysis(String... periods) {
        try {
            // ACTIVE 상태인 파트너사 목록 조회
            List<PartnerCompany> activePartners = partnerCompanyRepository.findByStatus(PartnerCompanyStatus.ACTIVE);
            log.info("ACTIVE 상태 파트너사 총 {}개 발견", activePartners.size());

            if (activePartners.isEmpty()) {
                log.info("ACTIVE 상태인 파트너사가 없어 뉴스 크롤링을 건너뜁니다.");
                return;
            }

            // 회사명 기준으로 중복 제거 (동일한 회사가 여러 파트너사로 등록된 경우)
            Set<String> uniqueCompanyNames = activePartners.stream()
                    .map(PartnerCompany::getCompanyName)
                    .filter(name -> name != null && !name.trim().isEmpty())
                    .map(String::trim)
                    .collect(Collectors.toSet());

            log.info("중복 제거 후 고유 회사명 {}개: {}", uniqueCompanyNames.size(), uniqueCompanyNames);

            // 각 고유 회사명에 대해 뉴스 크롤링 요청
            int successCount = 0;
            int failureCount = 0;

            for (String companyName : uniqueCompanyNames) {
                try {
                    // 해당 회사명의 대표 파트너사 정보 가져오기 (첫 번째 것 사용)
                    PartnerCompany representativePartner = activePartners.stream()
                            .filter(p -> companyName.equals(p.getCompanyName()))
                            .findFirst()
                            .orElse(null);

                    if (representativePartner == null) {
                        log.warn("회사명 '{}'에 해당하는 파트너사를 찾을 수 없습니다.", companyName);
                        failureCount++;
                        continue;
                    }

                    // 뉴스 크롤링 요청 메시지 생성
                    NewsAnalysisRequest newsRequest = NewsAnalysisRequest.builder()
                            .keyword(companyName)
                            .periods(List.of(periods))
                            .sources(List.of("naver")) // 네이버 뉴스
                            .partnerId(representativePartner.getId())
                            .corpCode(representativePartner.getCorpCode())
                            .requestedAt(LocalDateTime.now().toString())
                            .build();

                    // 비동기로 뉴스 크롤링 요청 전송
                    kafkaProducerService.sendMessage(newsKeywordsTopic, companyName, newsRequest)
                            .whenComplete((result, ex) -> {
                                if (ex == null) {
                                    log.debug("스케줄 뉴스 크롤링 요청 전송 성공: {}", companyName);
                                } else {
                                    log.error("스케줄 뉴스 크롤링 요청 전송 실패: {}", companyName, ex);
                                }
                            });

                    successCount++;
                    
                    // API 호출 간격 조절 (과부하 방지)
                    Thread.sleep(100); // 100ms 대기

                } catch (Exception e) {
                    log.error("회사명 '{}'에 대한 뉴스 크롤링 요청 중 오류 발생", companyName, e);
                    failureCount++;
                }
            }

            log.info("=== 파트너사 일일/주간 뉴스 크롤링 스케줄 완료 === 성공: {}, 실패: {}", successCount, failureCount);

        } catch (Exception e) {
            log.error("파트너사 뉴스 크롤링 스케줄 실행 중 오류 발생", e);
        }
    }

    /**
     * 수동으로 모든 파트너사 뉴스 크롤링 실행 (테스트/관리 목적)
     */
    public void executeManualNewsAnalysis() {
        log.info("=== 수동 파트너사 뉴스 크롤링 실행 ===");
        executeNewsAnalysis("1d", "1w"); // 최근 1일, 1주일
    }

    /**
     * 스케줄러 활성화/비활성화 상태 확인
     */
    public boolean isSchedulerEnabled() {
        return schedulerEnabled;
    }
} 