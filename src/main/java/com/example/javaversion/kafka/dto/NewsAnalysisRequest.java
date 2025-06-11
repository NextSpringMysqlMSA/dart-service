package com.example.javaversion.kafka.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * 뉴스 크롤링 및 분석 요청을 위한 DTO
 * news-keywords 토픽으로 전송되는 메시지 형식
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewsAnalysisRequest {
    
    /**
     * 검색할 키워드 (회사명)
     */
    @JsonProperty("keyword")
    private String keyword;
    
    /**
     * 검색 기간 목록
     * 유효한 값: "1d", "1w", "1m", "3m", "6m", "1y", "all"
     */
    @JsonProperty("periods")
    private List<String> periods;
    
    /**
     * 크롤링할 뉴스 소스 목록 (선택적)
     * 유효한 값: "naver", "google-news"
     */
    @JsonProperty("sources")
    private List<String> sources;
    
    /**
     * 파트너사 ID (추적용)
     */
    @JsonProperty("partner_id")
    private String partnerId;
    
    /**
     * DART 기업 고유번호 (선택적)
     */
    @JsonProperty("corp_code")
    private String corpCode;
    
    /**
     * 요청 시간
     */
    @JsonProperty("requested_at")
    private String requestedAt;
} 