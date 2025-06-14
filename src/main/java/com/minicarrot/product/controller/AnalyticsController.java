package com.minicarrot.product.controller;

import com.minicarrot.product.dto.event.AnalyticsEventDto;
import com.minicarrot.product.service.AnalyticsService;
import com.minicarrot.product.service.EventPublisherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final EventPublisherService eventPublisherService;
    
    // SSE 연결을 관리하기 위한 맵
    private final Map<String, SseEmitter> sseEmitters = new ConcurrentHashMap<>();

    // 지원되는 카테고리 목록
    private static final Set<String> SUPPORTED_CATEGORIES = Set.of(
        "전자제품", "패션잡화", "유아용품", "스포츠용품", "식품", "신발"
    );
    
    // 카테고리 매핑 (지원하지 않는 카테고리를 지원하는 카테고리로 변환)
    private static final Map<String, String> CATEGORY_MAPPING = Map.of(
        "의류", "패션잡화",
        "전자기기", "전자제품", 
        "아기용품", "유아용품",
        "운동용품", "스포츠용품",
        "음식", "식품"
    );
    
    /**
     * 카테고리 정규화 (지원하지 않는 카테고리를 지원하는 카테고리로 매핑)
     */
    private String normalizeCategory(String category) {
        if (category == null || category.trim().isEmpty()) {
            return "기타";
        }
        
        String trimmedCategory = category.trim();
        
        // 이미 지원되는 카테고리인 경우
        if (SUPPORTED_CATEGORIES.contains(trimmedCategory)) {
            return trimmedCategory;
        }
        
        // 매핑 테이블에서 찾기
        String mappedCategory = CATEGORY_MAPPING.get(trimmedCategory);
        if (mappedCategory != null) {
            log.debug("카테고리 매핑: {} -> {}", trimmedCategory, mappedCategory);
            return mappedCategory;
        }
        
        // 매핑되지 않는 카테고리는 "기타"로 처리
        log.debug("지원하지 않는 카테고리를 '기타'로 매핑: {}", trimmedCategory);
        return "기타";
    }

    /**
     * SSE 스트림 엔드포인트 - 실시간 분석 데이터 스트리밍
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamAnalytics() {
        String emitterId = UUID.randomUUID().toString();
        // 타임아웃을 10분으로 설정 (600,000ms)
        SseEmitter emitter = new SseEmitter(600_000L);
        
        sseEmitters.put(emitterId, emitter);
        
        emitter.onCompletion(() -> {
            log.info("SSE connection completed for emitter: {} (남은 연결 수: {})", emitterId, sseEmitters.size());
            cleanupSseConnection(emitterId, null);
        });
        
        emitter.onTimeout(() -> {
            log.info("SSE connection timeout for emitter: {} (남은 연결 수: {})", emitterId, sseEmitters.size());
            cleanupSseConnection(emitterId, null);
        });
        
        emitter.onError((ex) -> {
            log.error("SSE connection error for emitter: {} (남은 연결 수: {})", emitterId, sseEmitters.size(), ex);
            cleanupSseConnection(emitterId, null);
        });
        
        try {
            // 초기 연결 확인 메시지 전송
            Map<String, Object> initialData = new HashMap<>();
            initialData.put("type", "connection");
            initialData.put("message", "Connected to analytics stream");
            initialData.put("timestamp", System.currentTimeMillis());
            
            emitter.send(SseEmitter.event()
                    .id(emitterId)
                    .name("connection")
                    .data(initialData));
            
            // 주기적으로 실시간 통계 데이터 전송
            sendRealTimeStats(emitter, emitterId);
            
        } catch (IOException e) {
            log.error("Failed to send initial SSE message", e);
            sseEmitters.remove(emitterId);
            emitter.completeWithError(e);
        }
        
        return emitter;
    }
    
    /**
     * 실시간 통계 데이터를 SSE로 전송
     */
    private void sendRealTimeStats(SseEmitter emitter, String emitterId) {
        // 별도 스레드에서 주기적으로 데이터 전송
        new Thread(() -> {
            try {
                int failureCount = 0;
                int maxIterations = 120; // 최대 10분 (5초 * 120)
                int iterations = 0;
                
                while (sseEmitters.containsKey(emitterId) && failureCount < 3 && iterations < maxIterations) {
                    try {
                        // 연결 상태 확인
                        if (!sseEmitters.containsKey(emitterId)) {
                            log.debug("SSE 연결이 제거됨: {}", emitterId);
                            break;
                        }
                        
                        Map<String, Object> stats = analyticsService.getDashboardStats();
                        stats.put("timestamp", System.currentTimeMillis());
                        
                        emitter.send(SseEmitter.event()
                                .id(UUID.randomUUID().toString())
                                .name("stats")
                                .data(stats));
                        
                        failureCount = 0; // 성공 시 실패 카운트 리셋
                        iterations++;
                        Thread.sleep(5000); // 5초마다 업데이트
                        
                    } catch (IOException e) {
                        failureCount++;
                        log.warn("SSE 데이터 전송 실패 (시도 {}/3): {}", failureCount, e.getMessage());
                        
                        if (failureCount >= 3) {
                            log.info("SSE 연결 종료 - 클라이언트 연결 끊김: {}", emitterId);
                            break;
                        }
                        
                        // 연결이 끊어진 경우 즉시 정리
                        if (e.getMessage() != null && e.getMessage().contains("Broken pipe")) {
                            log.info("Broken pipe 감지 - 즉시 연결 정리: {}", emitterId);
                            break;
                        }
                        
                        Thread.sleep(1000); // 재시도 전 잠시 대기
                    }
                }
                
                if (iterations >= maxIterations) {
                    log.info("SSE 연결 시간 초과로 종료: {}", emitterId);
                }
                
            } catch (InterruptedException e) {
                log.info("SSE 스레드 중단됨: {}", emitterId);
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                log.error("SSE 스레드에서 예상치 못한 오류 발생: {}", emitterId, e);
            } finally {
                // 정리 작업
                cleanupSseConnection(emitterId, emitter);
            }
        }, "SSE-Analytics-" + emitterId).start();
    }
    
    /**
     * SSE 연결 정리
     */
    private void cleanupSseConnection(String emitterId, SseEmitter emitter) {
        try {
            sseEmitters.remove(emitterId);
            if (emitter != null) {
                emitter.complete();
            }
            log.debug("SSE 연결 정리 완료: {}", emitterId);
        } catch (Exception e) {
            log.debug("SSE 연결 정리 중 오류 (무시 가능): {}", e.getMessage());
        }
    }

    /**
     * 상품 조회 이벤트 발행 (POST) - JSON body 방식
     */
    @PostMapping("/view/{productId}")
    public ResponseEntity<Map<String, Object>> trackProductView(
            @PathVariable Long productId,
            @RequestBody ViewTrackingRequest viewRequest,
            HttpServletRequest request) {
        
        try {
            // userId 처리
            Long userIdLong = null;
            if (viewRequest.getUserId() != null && !viewRequest.getUserId().equals("anonymous") && !viewRequest.getUserId().isEmpty()) {
                try {
                    userIdLong = Long.parseLong(viewRequest.getUserId());
                } catch (NumberFormatException e) {
                    log.debug("Invalid userId format: {}", viewRequest.getUserId());
                }
            }
            
            AnalyticsEventDto analytics = AnalyticsEventDto.builder()
                    .eventType("VIEW")
                    .productId(productId)
                    .productCategory(normalizeCategory(viewRequest.getCategory()))
                    .userId(userIdLong)
                    .sessionId(request.getSession().getId())
                    .userAgent(request.getHeader("User-Agent"))
                    .ipAddress(getClientIpAddress(request))
                    .referrer(request.getHeader("Referer"))
                    .currentPage(request.getRequestURL().toString())
                    .build();

            // 비동기로 분석 이벤트 발행
            eventPublisherService.publishViewAnalytics(analytics);
            
            // 즉시 조회수 증가 (동기적으로 처리)
            analyticsService.saveViewAnalytics(analytics);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "View tracked successfully");
            response.put("productId", productId);
            response.put("category", normalizeCategory(viewRequest.getCategory()));
            
            return ResponseEntity.ok()
                    .header("Content-Type", "application/json;charset=UTF-8")
                    .body(response);
        } catch (Exception e) {
            log.error("Failed to track product view", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to track view");
            errorResponse.put("productId", productId);
            return ResponseEntity.internalServerError()
                    .header("Content-Type", "application/json;charset=UTF-8")
                    .body(errorResponse);
        }
    }

    /**
     * 상품 조회 이벤트 발행 (GET) - 프론트엔드 호환성을 위해 추가
     */
    @GetMapping("/view/{productId}")
    public ResponseEntity<String> trackProductViewGet(
            @PathVariable Long productId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String userId,
            HttpServletRequest request) {
        
        try {
            // userId를 String에서 Long으로 변환
            Long userIdLong = null;
            if (userId != null && !userId.equals("anonymous") && !userId.isEmpty()) {
                try {
                    userIdLong = Long.parseLong(userId);
                } catch (NumberFormatException e) {
                    log.debug("Invalid userId format: {}", userId);
                }
            }
            
            AnalyticsEventDto analytics = AnalyticsEventDto.builder()
                    .eventType("VIEW")
                    .productId(productId)
                    .productCategory(normalizeCategory(category))
                    .userId(userIdLong)
                    .sessionId(request.getSession().getId())
                    .userAgent(request.getHeader("User-Agent"))
                    .ipAddress(getClientIpAddress(request))
                    .referrer(request.getHeader("Referer"))
                    .currentPage(request.getRequestURL().toString())
                    .build();

            // 비동기로 분석 이벤트 발행
            eventPublisherService.publishViewAnalytics(analytics);
            
            return ResponseEntity.ok("View tracked successfully");
        } catch (Exception e) {
            log.error("Failed to track product view", e);
            return ResponseEntity.internalServerError().body("Failed to track view");
        }
    }

    /**
     * 검색 이벤트 발행
     */
    @PostMapping("/search")
    public ResponseEntity<String> trackSearch(
            @RequestBody SearchTrackingRequest request,
            HttpServletRequest httpRequest) {
        
        log.info("검색 추적 요청 받음: keyword={}, category={}", request.getKeyword(), request.getCategory());
        try {
            AnalyticsEventDto analytics = AnalyticsEventDto.builder()
                    .eventType("SEARCH")
                    .searchKeyword(request.getKeyword())
                    .searchCategory(normalizeCategory(request.getCategory()))
                    .searchResultCount(request.getResultCount())
                    .userId(request.getUserId())
                    .sessionId(httpRequest.getSession().getId())
                    .userAgent(httpRequest.getHeader("User-Agent"))
                    .ipAddress(getClientIpAddress(httpRequest))
                    .build();

            // 비동기로 검색 분석 이벤트 발행
            eventPublisherService.publishSearchAnalytics(analytics);
            
            // 즉시 검색 통계 증가 (동기적으로 처리)
            analyticsService.saveSearchAnalytics(analytics);
            log.info("검색 추적 완료: keyword={}", request.getKeyword());
            
            return ResponseEntity.ok("Search tracked successfully");
        } catch (Exception e) {
            log.error("Failed to track search", e);
            return ResponseEntity.internalServerError().body("Failed to track search");
        }
    }

    /**
     * 상품별 조회수 조회
     */
    @GetMapping("/product/{productId}/views")
    public ResponseEntity<Map<String, Object>> getProductViewCount(@PathVariable Long productId) {
        try {
            Long viewCount = analyticsService.getProductViewCount(productId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("productId", productId);
            response.put("viewCount", viewCount);
            
            // chunked encoding 문제 해결을 위해 명시적 헤더 설정
            return ResponseEntity.ok()
                    .header("Content-Type", "application/json;charset=UTF-8")
                    .header("Cache-Control", "no-cache")
                    .body(response);
        } catch (Exception e) {
            log.error("Failed to get product view count", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to get view count");
            errorResponse.put("productId", productId);
            errorResponse.put("viewCount", 0);
            return ResponseEntity.internalServerError()
                    .header("Content-Type", "application/json;charset=UTF-8")
                    .body(errorResponse);
        }
    }

    /**
     * 검색어별 검색 횟수 조회
     */
    @GetMapping("/search/{keyword}/count")
    public ResponseEntity<Map<String, Object>> getSearchKeywordCount(@PathVariable String keyword) {
        try {
            Long searchCount = analyticsService.getSearchKeywordCount(keyword);
            
            Map<String, Object> response = new HashMap<>();
            response.put("keyword", keyword);
            response.put("searchCount", searchCount);
            
            return ResponseEntity.ok()
                    .header("Content-Type", "application/json;charset=UTF-8")
                    .header("Cache-Control", "no-cache")
                    .body(response);
        } catch (Exception e) {
            log.error("Failed to get search keyword count", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to get search count");
            errorResponse.put("keyword", keyword);
            errorResponse.put("searchCount", 0);
            return ResponseEntity.internalServerError()
                    .header("Content-Type", "application/json;charset=UTF-8")
                    .body(errorResponse);
        }
    }

    /**
     * 카테고리별 통계 조회
     */
    @GetMapping("/category/{category}/stats")
    public ResponseEntity<Map<String, Object>> getCategoryStats(@PathVariable String category) {
        try {
            Long categoryCount = analyticsService.getCategoryCount(normalizeCategory(category));
            Long searchCount = analyticsService.getCategoryCount("search_" + normalizeCategory(category));
            
            Map<String, Object> response = new HashMap<>();
            response.put("category", normalizeCategory(category));
            response.put("viewCount", categoryCount);
            response.put("searchCount", searchCount);
            
            return ResponseEntity.ok()
                    .header("Content-Type", "application/json;charset=UTF-8")
                    .header("Cache-Control", "no-cache")
                    .body(response);
        } catch (Exception e) {
            log.error("Failed to get category stats", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to get category stats");
            errorResponse.put("category", normalizeCategory(category));
            errorResponse.put("viewCount", 0);
            errorResponse.put("searchCount", 0);
            return ResponseEntity.internalServerError()
                    .header("Content-Type", "application/json;charset=UTF-8")
                    .body(errorResponse);
        }
    }

    /**
     * 인기 상품 순위 API
     */
    @GetMapping("/popular-products")
    public ResponseEntity<Map<String, Object>> getPopularProducts(
            @RequestParam(defaultValue = "10") int limit) {
        
        try {
            Map<String, Object> response = analyticsService.getPopularProducts(limit);
            return ResponseEntity.ok()
                    .header("Content-Type", "application/json;charset=UTF-8")
                    .header("Cache-Control", "no-cache")
                    .body(response);
        } catch (Exception e) {
            log.error("Failed to get popular products", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to get popular products");
            errorResponse.put("products", new ArrayList<>());
            return ResponseEntity.internalServerError()
                    .header("Content-Type", "application/json;charset=UTF-8")
                    .body(errorResponse);
        }
    }

    /**
     * 🚀 실시간 통계 대시보드 API (초고속 병렬 처리)
     */
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        try {
            // 대시보드 통계 조회
            Map<String, Object> response = analyticsService.getDashboardStats();
            return ResponseEntity.ok()
                    .header("Content-Type", "application/json;charset=UTF-8")
                    .header("Cache-Control", "no-cache")
                    .body(response);
        } catch (Exception e) {
            log.error("Failed to get dashboard stats", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to get dashboard stats");
            errorResponse.put("totalProducts", 0);
            errorResponse.put("totalViews", 0);
            return ResponseEntity.internalServerError()
                    .header("Content-Type", "application/json;charset=UTF-8")
                    .body(errorResponse);
        }
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }

    // 검색 추적 요청 DTO
    public static class SearchTrackingRequest {
        private String keyword;
        private String category;
        private Integer resultCount;
        private Long userId;
        
        // Getters and Setters
        public String getKeyword() { return keyword; }
        public void setKeyword(String keyword) { this.keyword = keyword; }
        
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        
        public Integer getResultCount() { return resultCount; }
        public void setResultCount(Integer resultCount) { this.resultCount = resultCount; }
        
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
    }
    
    // 조회수 추적 요청 DTO
    public static class ViewTrackingRequest {
        private String category;
        private String userId;
        
        // Getters and Setters
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
    }
} 