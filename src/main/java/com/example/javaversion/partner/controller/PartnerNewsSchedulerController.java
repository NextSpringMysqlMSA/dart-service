package com.example.javaversion.partner.controller;

import com.example.javaversion.partner.service.PartnerNewsSchedulerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 파트너사 뉴스 스케줄러 관리 컨트롤러
 * 뉴스 크롤링 스케줄러의 수동 실행 및 상태 확인 기능을 제공합니다.
 */
@RestController
@RequestMapping("/api/v1/partner-news-scheduler")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Partner News Scheduler", description = "파트너사 뉴스 크롤링 스케줄러 관리 API")
public class PartnerNewsSchedulerController {

    private final PartnerNewsSchedulerService partnerNewsSchedulerService;

    /**
     * 스케줄러 상태 확인
     */
    @GetMapping("/status")
    @Operation(summary = "스케줄러 상태 확인", description = "파트너사 뉴스 크롤링 스케줄러의 활성화 상태를 확인합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "스케줄러 상태 조회 성공")
    })
    public ResponseEntity<Map<String, Object>> getSchedulerStatus() {
        log.info("스케줄러 상태 확인 요청");
        
        boolean isEnabled = partnerNewsSchedulerService.isSchedulerEnabled();
        
        Map<String, Object> status = Map.of(
                "enabled", isEnabled,
                "description", isEnabled ? "스케줄러가 활성화되어 있습니다." : "스케줄러가 비활성화되어 있습니다.",
                "dailySchedule", "매일 오전 9시",
                "weeklySchedule", "매주 월요일 오전 8시",
                "timestamp", LocalDateTime.now().toString()
        );
        
        return ResponseEntity.ok(status);
    }

    /**
     * 수동으로 모든 파트너사 뉴스 크롤링 실행
     */
    @PostMapping("/execute")
    @Operation(summary = "수동 뉴스 크롤링 실행", description = "모든 ACTIVE 상태 파트너사에 대해 즉시 뉴스 크롤링을 실행합니다. 개발 및 테스트 목적으로 사용됩니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "뉴스 크롤링 실행 성공"),
            @ApiResponse(responseCode = "500", description = "뉴스 크롤링 실행 중 오류 발생")
    })
    public ResponseEntity<Map<String, Object>> executeManualNewsAnalysis() {
        log.info("수동 파트너사 뉴스 크롤링 실행 요청");
        
        try {
            partnerNewsSchedulerService.executeManualNewsAnalysis();
            
            Map<String, Object> response = Map.of(
                    "message", "모든 ACTIVE 파트너사에 대한 뉴스 크롤링 요청이 전송되었습니다.",
                    "executedAt", LocalDateTime.now().toString(),
                    "type", "manual_execution"
            );
            
            log.info("수동 파트너사 뉴스 크롤링 실행 완료");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("수동 파트너사 뉴스 크롤링 실행 중 오류 발생", e);
            
            Map<String, Object> errorResponse = Map.of(
                    "error", "뉴스 크롤링 실행 중 오류가 발생했습니다: " + e.getMessage(),
                    "timestamp", LocalDateTime.now().toString()
            );
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 일일 스케줄 수동 실행 (테스트용)
     */
    @PostMapping("/execute/daily")
    @Operation(summary = "일일 스케줄 수동 실행", description = "일일 뉴스 크롤링 스케줄을 수동으로 실행합니다. 테스트 목적으로 사용됩니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "일일 스케줄 실행 성공"),
            @ApiResponse(responseCode = "500", description = "일일 스케줄 실행 중 오류 발생")
    })
    public ResponseEntity<Map<String, Object>> executeDailySchedule() {
        log.info("일일 뉴스 크롤링 스케줄 수동 실행 요청");
        
        try {
            partnerNewsSchedulerService.scheduleDailyNewsAnalysis();
            
            Map<String, Object> response = Map.of(
                    "message", "일일 뉴스 크롤링 스케줄이 수동으로 실행되었습니다.",
                    "executedAt", LocalDateTime.now().toString(),
                    "type", "daily_schedule",
                    "periods", "1d, 1w"
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("일일 뉴스 크롤링 스케줄 수동 실행 중 오류 발생", e);
            
            Map<String, Object> errorResponse = Map.of(
                    "error", "일일 스케줄 실행 중 오류가 발생했습니다: " + e.getMessage(),
                    "timestamp", LocalDateTime.now().toString()
            );
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * 주간 스케줄 수동 실행 (테스트용)
     */
    @PostMapping("/execute/weekly")
    @Operation(summary = "주간 스케줄 수동 실행", description = "주간 뉴스 크롤링 스케줄을 수동으로 실행합니다. 테스트 목적으로 사용됩니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "주간 스케줄 실행 성공"),
            @ApiResponse(responseCode = "500", description = "주간 스케줄 실행 중 오류 발생")
    })
    public ResponseEntity<Map<String, Object>> executeWeeklySchedule() {
        log.info("주간 뉴스 크롤링 스케줄 수동 실행 요청");
        
        try {
            partnerNewsSchedulerService.scheduleWeeklyNewsAnalysis();
            
            Map<String, Object> response = Map.of(
                    "message", "주간 뉴스 크롤링 스케줄이 수동으로 실행되었습니다.",
                    "executedAt", LocalDateTime.now().toString(),
                    "type", "weekly_schedule",
                    "periods", "1w, 1m"
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("주간 뉴스 크롤링 스케줄 수동 실행 중 오류 발생", e);
            
            Map<String, Object> errorResponse = Map.of(
                    "error", "주간 스케줄 실행 중 오류가 발생했습니다: " + e.getMessage(),
                    "timestamp", LocalDateTime.now().toString()
            );
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
} 