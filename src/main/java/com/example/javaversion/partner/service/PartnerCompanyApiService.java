/**
 * @file PartnerCompanyApiService.java
 * @description 파트너 회사 API와 통신하기 위한 서비스입니다.
 *              파트너 회사 정보의 CRUD 기능을 제공합니다.
 */
package com.example.javaversion.partner.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;

import com.example.javaversion.dart.dto.CompanyProfileResponse;
import com.example.javaversion.dart.service.DartApiService;
import com.example.javaversion.database.entity.PartnerCompany;
import com.example.javaversion.database.repository.PartnerCompanyRepository;
import com.example.javaversion.kafka.service.KafkaProducerService;
import com.example.javaversion.partner.dto.CreatePartnerCompanyDto;
import com.example.javaversion.partner.dto.PaginatedPartnerCompanyResponseDto;
import com.example.javaversion.partner.dto.PartnerCompanyResponseDto;
import com.example.javaversion.partner.dto.UpdatePartnerCompanyDto;
import com.example.javaversion.partner.model.PartnerCompanyStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PartnerCompanyApiService {

    @Value("${partner.api.base-url}")
    private String baseUrl;

    @Value("${partner.api.client-id}")
    private String clientId;

    @Value("${partner.api.client-secret}")
    private String clientSecret;

    @Value("${kafka.topic.partner-company}")
    private String partnerCompanyTopic;

    private final WebClient.Builder webClientBuilder;
    private final PartnerCompanyRepository partnerCompanyRepository;
    private final DartApiService dartApiService;
    private final KafkaProducerService kafkaProducerService;

    /**
     * 파트너 API에서 회사 정보를 조회합니다.
     * 
     * @param companyId 회사 ID
     * @return 회사 정보
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getCompanyInfo(String companyId) {
        log.info("파트너 API 호출 - 회사 정보 조회: {}", companyId);

        return webClientBuilder.baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("X-Client-Id", clientId)
                .defaultHeader("X-Client-Secret", clientSecret)
                .build()
                .get()
                .uri("/companies/{companyId}", companyId)
                .retrieve()
                .bodyToMono(Map.class)
                .doOnError(e -> log.error("파트너 API 호출 실패: {}", e.getMessage()))
                .block();
    }

    /**
     * 파트너 API에서 회사의 재무 정보를 조회합니다.
     * 
     * @param companyId 회사 ID
     * @param year 조회 연도
     * @param quarter 조회 분기
     * @return 재무 정보
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getFinancialInfo(String companyId, int year, int quarter) {
        log.info("파트너 API 호출 - 재무 정보 조회: {}, {}년 {}분기", companyId, year, quarter);

        return webClientBuilder.baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("X-Client-Id", clientId)
                .defaultHeader("X-Client-Secret", clientSecret)
                .build()
                .get()
                .uri("/companies/{companyId}/financials?year={year}&quarter={quarter}", 
                        companyId, year, quarter)
                .retrieve()
                .bodyToMono(Map.class)
                .doOnError(e -> log.error("파트너 API 호출 실패: {}", e.getMessage()))
                .block();
    }

    /**
     * 새로운 파트너 회사를 생성합니다.
     * 
     * @param createDto 생성할 파트너 회사 정보
     * @param memberId 요청한 회원 ID
     * @return 생성된 파트너 회사 정보
     */
    @Transactional
    public PartnerCompanyResponseDto createPartnerCompany(CreatePartnerCompanyDto createDto, String memberId) {
        log.info("파트너 회사 생성 요청 - 회사명: {}, 회원 ID: {}", createDto.getCompanyName(), memberId);

        // DART API에서 추가 정보 조회
        String stockCode = null;
        String finalCorpCode = createDto.getCorpCode();

        try {
            CompanyProfileResponse dartProfile = dartApiService.getCompanyProfile(createDto.getCorpCode()).block();
            if (dartProfile != null) {
                log.info("DART API에서 회사 정보 조회 성공 - 회사명: {}", dartProfile.getCorpName());
                stockCode = dartProfile.getStockCode();

                // DART API에서 반환한 corpCode가 다른 경우 업데이트
                if (dartProfile.getCorpCode() != null && !dartProfile.getCorpCode().equals(finalCorpCode)) {
                    log.info("DART API에서 반환한 corpCode({})가 요청한 corpCode({})와 다릅니다. DART API 값을 사용합니다.", 
                            dartProfile.getCorpCode(), finalCorpCode);
                    finalCorpCode = dartProfile.getCorpCode();
                }
            }
        } catch (Exception e) {
            log.warn("DART API 호출 중 오류 발생: {}", e.getMessage());
        }

        // 파트너 회사 엔티티 생성
        PartnerCompany partnerCompany = PartnerCompany.builder()
                .memberId(memberId)
                .companyName(createDto.getCompanyName())
                .corpCode(finalCorpCode)
                .stockCode(stockCode)
                .contractStartDate(createDto.getContractStartDate())
                .status(PartnerCompanyStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // 데이터베이스에 저장
        PartnerCompany savedPartnerCompany = partnerCompanyRepository.save(partnerCompany);
        log.info("파트너 회사 생성 완료 - ID: {}", savedPartnerCompany.getId());

        // 응답 DTO 생성
        PartnerCompanyResponseDto responseDto = mapToResponseDto(savedPartnerCompany);

        // Kafka 메시지 발행
        try {
            CompletableFuture<SendResult<String, Object>> future = 
                    kafkaProducerService.sendMessage(partnerCompanyTopic, savedPartnerCompany.getId().toString(), responseDto);
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("파트너 회사 생성 메시지 발행 성공 - ID: {}", savedPartnerCompany.getId());
                } else {
                    log.error("파트너 회사 생성 메시지 발행 실패 - ID: {}", savedPartnerCompany.getId(), ex);
                }
            });
        } catch (Exception e) {
            log.error("Kafka 메시지 발행 중 오류 발생", e);
        }

        return responseDto;
    }

    /**
     * 파트너 회사 목록을 조회합니다.
     * 
     * @param page 페이지 번호 (1부터 시작)
     * @param pageSize 페이지당 항목 수
     * @param companyName 회사명 필터 (부분 일치)
     * @return 페이지네이션을 포함한 파트너 회사 목록
     */
    @Transactional(readOnly = true)
    public PaginatedPartnerCompanyResponseDto findAllPartnerCompanies(int page, int pageSize, String companyName) {
        log.info("파트너 회사 목록 조회 요청 - 페이지: {}, 페이지 크기: {}, 회사명 필터: {}", page, pageSize, companyName);

        // 페이지 번호는 0부터 시작하므로 1을 빼줌
        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<PartnerCompany> partnerCompaniesPage;
        if (companyName != null && !companyName.isEmpty()) {
            partnerCompaniesPage = partnerCompanyRepository.findByCompanyNameContainingIgnoreCaseAndStatus(
                    companyName, PartnerCompanyStatus.ACTIVE, pageable);
        } else {
            partnerCompaniesPage = partnerCompanyRepository.findByStatus(PartnerCompanyStatus.ACTIVE, pageable);
        }

        List<PartnerCompanyResponseDto> partnerCompanies = partnerCompaniesPage.getContent().stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());

        return PaginatedPartnerCompanyResponseDto.builder()
                .data(partnerCompanies)
                .total(partnerCompaniesPage.getTotalElements())
                .page(page)
                .pageSize(pageSize)
                .build();
    }

    /**
     * ID로 파트너 회사를 조회합니다.
     * 
     * @param id 파트너 회사 ID
     * @return 파트너 회사 정보
     * @throws ResponseStatusException 파트너 회사를 찾을 수 없는 경우
     */
    @Transactional(readOnly = true)
    public PartnerCompanyResponseDto findPartnerCompanyById(UUID id) {
        log.info("파트너 회사 조회 요청 - ID: {}", id);

        PartnerCompany partnerCompany = partnerCompanyRepository.findByIdAndStatus(id, PartnerCompanyStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
                        String.format("ID '%s'에 해당하는 파트너사를 찾을 수 없습니다.", id)));

        return mapToResponseDto(partnerCompany);
    }

    /**
     * 파트너 회사 정보를 업데이트합니다.
     * 
     * @param id 파트너 회사 ID
     * @param updateDto 업데이트할 파트너 회사 정보
     * @return 업데이트된 파트너 회사 정보
     * @throws ResponseStatusException 파트너 회사를 찾을 수 없는 경우
     */
    @Transactional
    public PartnerCompanyResponseDto updatePartnerCompany(UUID id, UpdatePartnerCompanyDto updateDto) {
        log.info("파트너 회사 업데이트 요청 - ID: {}", id);

        PartnerCompany partnerCompany = partnerCompanyRepository.findByIdAndStatus(id, PartnerCompanyStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
                        String.format("ID '%s'에 해당하는 파트너사를 찾을 수 없습니다.", id)));

        // DART API에서 추가 정보 조회 (corpCode가 변경된 경우)
        String stockCode = partnerCompany.getStockCode();
        String finalCorpCode = updateDto.getCorpCode() != null ? updateDto.getCorpCode() : partnerCompany.getCorpCode();

        if (updateDto.getCorpCode() != null && !updateDto.getCorpCode().equals(partnerCompany.getCorpCode())) {
            try {
                CompanyProfileResponse dartProfile = dartApiService.getCompanyProfile(updateDto.getCorpCode()).block();
                if (dartProfile != null) {
                    log.info("DART API에서 회사 정보 조회 성공 - 회사명: {}", dartProfile.getCorpName());
                    stockCode = dartProfile.getStockCode();

                    // DART API에서 반환한 corpCode가 다른 경우 업데이트
                    if (dartProfile.getCorpCode() != null && !dartProfile.getCorpCode().equals(finalCorpCode)) {
                        log.info("DART API에서 반환한 corpCode({})가 요청한 corpCode({})와 다릅니다. DART API 값을 사용합니다.", 
                                dartProfile.getCorpCode(), finalCorpCode);
                        finalCorpCode = dartProfile.getCorpCode();
                    }
                }
            } catch (Exception e) {
                log.warn("DART API 호출 중 오류 발생: {}", e.getMessage());
            }
        }

        // 필드 업데이트
        if (updateDto.getCompanyName() != null) {
            partnerCompany.setCompanyName(updateDto.getCompanyName());
        }

        partnerCompany.setCorpCode(finalCorpCode);
        partnerCompany.setStockCode(stockCode);

        if (updateDto.getContractStartDate() != null) {
            partnerCompany.setContractStartDate(updateDto.getContractStartDate());
        }

        partnerCompany.setUpdatedAt(LocalDateTime.now());

        // 데이터베이스에 저장
        PartnerCompany updatedPartnerCompany = partnerCompanyRepository.save(partnerCompany);
        log.info("파트너 회사 업데이트 완료 - ID: {}", updatedPartnerCompany.getId());

        // 응답 DTO 생성
        PartnerCompanyResponseDto responseDto = mapToResponseDto(updatedPartnerCompany);

        // Kafka 메시지 발행
        try {
            CompletableFuture<SendResult<String, Object>> future = 
                    kafkaProducerService.sendMessage(partnerCompanyTopic, updatedPartnerCompany.getId().toString(), responseDto);
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("파트너 회사 업데이트 메시지 발행 성공 - ID: {}", updatedPartnerCompany.getId());
                } else {
                    log.error("파트너 회사 업데이트 메시지 발행 실패 - ID: {}", updatedPartnerCompany.getId(), ex);
                }
            });
        } catch (Exception e) {
            log.error("Kafka 메시지 발행 중 오류 발생", e);
        }

        return responseDto;
    }

    /**
     * 파트너 회사를 삭제(비활성화)합니다.
     * 
     * @param id 파트너 회사 ID
     * @return 삭제 결과 메시지
     * @throws ResponseStatusException 파트너 회사를 찾을 수 없는 경우
     */
    @Transactional
    public Map<String, String> deletePartnerCompany(UUID id) {
        log.info("파트너 회사 삭제 요청 - ID: {}", id);

        PartnerCompany partnerCompany = partnerCompanyRepository.findByIdAndStatus(id, PartnerCompanyStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, 
                        String.format("ID '%s'에 해당하는 파트너사를 찾을 수 없습니다.", id)));

        // 상태를 INACTIVE로 변경 (소프트 삭제)
        partnerCompany.setStatus(PartnerCompanyStatus.INACTIVE);
        partnerCompany.setContractEndDate(LocalDateTime.now().toLocalDate());
        partnerCompany.setUpdatedAt(LocalDateTime.now());

        // 데이터베이스에 저장
        PartnerCompany deletedPartnerCompany = partnerCompanyRepository.save(partnerCompany);
        log.info("파트너 회사 삭제(비활성화) 완료 - ID: {}", deletedPartnerCompany.getId());

        // 응답 DTO 생성
        PartnerCompanyResponseDto responseDto = mapToResponseDto(deletedPartnerCompany);

        // Kafka 메시지 발행
        try {
            CompletableFuture<SendResult<String, Object>> future = 
                    kafkaProducerService.sendMessage(partnerCompanyTopic, deletedPartnerCompany.getId().toString(), responseDto);
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("파트너 회사 삭제 메시지 발행 성공 - ID: {}", deletedPartnerCompany.getId());
                } else {
                    log.error("파트너 회사 삭제 메시지 발행 실패 - ID: {}", deletedPartnerCompany.getId(), ex);
                }
            });
        } catch (Exception e) {
            log.error("Kafka 메시지 발행 중 오류 발생", e);
        }

        return Map.of("message",
                String.format("ID '%s' 파트너사가 성공적으로 비활성화되었습니다.", id));
    }

    /**
     * 파트너 회사 엔티티를 응답 DTO로 변환합니다.
     * 
     * @param partnerCompany 파트너 회사 엔티티
     * @return 파트너 회사 응답 DTO
     */
    private PartnerCompanyResponseDto mapToResponseDto(PartnerCompany partnerCompany) {
        if (partnerCompany == null) {
            return null;
        }

        return PartnerCompanyResponseDto.builder()
                .id(partnerCompany.getId())
                .corpCode(partnerCompany.getCorpCode())
                .corpName(partnerCompany.getCompanyName())
                .stockCode(partnerCompany.getStockCode())
                .contractStartDate(partnerCompany.getContractStartDate())
                .modifyDate(PartnerCompanyResponseDto.formatDateToYYYYMMDD(partnerCompany.getUpdatedAt()))
                .status(partnerCompany.getStatus())
                .industry(partnerCompany.getIndustry())
                .country(partnerCompany.getCountry())
                .address(partnerCompany.getAddress())
                .build();
    }

    /**
     * 특정 회원이 등록한 파트너 회사 목록을 조회합니다.
     * 
     * @param memberId 회원 ID
     * @param page 페이지 번호 (1부터 시작)
     * @param pageSize 페이지당 항목 수
     * @param companyName 회사명 필터 (부분 일치)
     * @return 페이지네이션을 포함한 파트너 회사 목록
     */
    @Transactional(readOnly = true)
    public PaginatedPartnerCompanyResponseDto findAllPartnerCompaniesByMemberId(String memberId, int page, int pageSize, String companyName) {
        log.info("회원 ID '{}'의 파트너 회사 목록 조회 요청 - 페이지: {}, 페이지 크기: {}, 회사명 필터: {}", memberId, page, pageSize, companyName);

        // 페이지 번호는 0부터 시작하므로 1을 빼줌
        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<PartnerCompany> partnerCompaniesPage;
        if (companyName != null && !companyName.isEmpty()) {
            partnerCompaniesPage = partnerCompanyRepository.findByMemberIdAndCompanyNameContainingIgnoreCaseAndStatus(
                    memberId, companyName, PartnerCompanyStatus.ACTIVE, pageable);
        } else {
            partnerCompaniesPage = partnerCompanyRepository.findByMemberIdAndStatus(
                    memberId, PartnerCompanyStatus.ACTIVE, pageable);
        }

        List<PartnerCompanyResponseDto> partnerCompanies = partnerCompaniesPage.getContent().stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());

        return PaginatedPartnerCompanyResponseDto.builder()
                .data(partnerCompanies)
                .total(partnerCompaniesPage.getTotalElements())
                .page(page)
                .pageSize(pageSize)
                .build();
    }

    /**
     * 시스템에 등록된 모든 활성 상태의 파트너사들의 고유한 회사명 목록을 조회합니다.
     * 
     * @return 고유한 파트너사명 목록
     */
    @Transactional(readOnly = true)
    public List<String> getUniqueActivePartnerCompanyNames() {
        log.info("모든 활성 파트너사의 고유한 회사명 목록 조회 요청");
        List<PartnerCompany> activeCompanies = partnerCompanyRepository.findByStatus(PartnerCompanyStatus.ACTIVE);
        return activeCompanies.stream()
                .map(PartnerCompany::getCompanyName)
                .distinct()
                .collect(Collectors.toList());
    }
} 
