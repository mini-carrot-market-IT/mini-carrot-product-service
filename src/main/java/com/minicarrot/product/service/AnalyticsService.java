package com.minicarrot.product.service;

import com.minicarrot.product.dto.event.AnalyticsEventDto;
import com.minicarrot.product.dto.ProductResponse;
import com.minicarrot.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final ProductRepository productRepository;
    
    // 실시간 통계를 위한 인메모리 캐시 (실제로는 Redis 등 사용)
    private final ConcurrentHashMap<Long, AtomicLong> productViewCounts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> searchKeywordCounts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> categoryCounts = new ConcurrentHashMap<>();

    public void saveViewAnalytics(AnalyticsEventDto analytics) {
        log.debug("Saving view analytics for product: {}", analytics.getProductId());
        
        try {
            // 실제로는 데이터베이스나 분석 플랫폼에 저장
            // 예: InfluxDB, Elasticsearch, BigQuery 등
            
            // 상품 조회수 업데이트
            if (analytics.getProductId() != null) {
                productViewCounts.computeIfAbsent(analytics.getProductId(), k -> new AtomicLong(0))
                                .incrementAndGet();
            }
            
            // 카테고리별 조회수 업데이트
            if (analytics.getProductCategory() != null) {
                categoryCounts.computeIfAbsent(analytics.getProductCategory(), k -> new AtomicLong(0))
                             .incrementAndGet();
            }
            
            log.debug("View analytics saved successfully for product: {}", analytics.getProductId());
        } catch (Exception e) {
            log.error("Failed to save view analytics", e);
        }
    }

    public void saveSearchAnalytics(AnalyticsEventDto analytics) {
        log.debug("Saving search analytics for keyword: {}", analytics.getSearchKeyword());
        
        try {
            // 검색 키워드 통계 업데이트
            if (analytics.getSearchKeyword() != null) {
                searchKeywordCounts.computeIfAbsent(analytics.getSearchKeyword(), k -> new AtomicLong(0))
                                  .incrementAndGet();
            }
            
            // 카테고리별 검색 통계 업데이트
            if (analytics.getSearchCategory() != null) {
                String categoryKey = "search_" + analytics.getSearchCategory();
                categoryCounts.computeIfAbsent(categoryKey, k -> new AtomicLong(0))
                             .incrementAndGet();
            }
            
            log.debug("Search analytics saved successfully for keyword: {}", analytics.getSearchKeyword());
        } catch (Exception e) {
            log.error("Failed to save search analytics", e);
        }
    }

    public void updateRealTimeStats(AnalyticsEventDto analytics) {
        log.debug("Updating real-time stats for product: {}", analytics.getProductId());
        
        try {
            // 실시간 대시보드나 캐시 업데이트
            // 예: Redis, Hazelcast 등을 활용한 실시간 통계 업데이트
            
            // 인기 상품 순위 업데이트
            updatePopularProducts(analytics);
            
            // 실시간 활동 통계 업데이트
            updateRealtimeActivity(analytics);
            
        } catch (Exception e) {
            log.error("Failed to update real-time stats", e);
        }
    }

    public void updatePopularKeywords(AnalyticsEventDto analytics) {
        log.debug("Updating popular keywords for: {}", analytics.getSearchKeyword());
        
        try {
            // 인기 검색어 순위 업데이트
            // 실제로는 시간대별, 일별 인기 검색어 통계를 관리
            
            String keyword = analytics.getSearchKeyword();
            if (keyword != null && !keyword.trim().isEmpty()) {
                Long count = searchKeywordCounts.get(keyword).get();
                
                // 상위 검색어 목록 업데이트 로직
                updateTopSearchKeywords(keyword, count);
            }
            
        } catch (Exception e) {
            log.error("Failed to update popular keywords", e);
        }
    }

    // 상품별 조회수 조회
    public Long getProductViewCount(Long productId) {
        AtomicLong count = productViewCounts.get(productId);
        return count != null ? count.get() : 0L;
    }

    // 검색어별 검색 횟수 조회
    public Long getSearchKeywordCount(String keyword) {
        AtomicLong count = searchKeywordCounts.get(keyword);
        return count != null ? count.get() : 0L;
    }

    // 카테고리별 통계 조회
    public Long getCategoryCount(String category) {
        AtomicLong count = categoryCounts.get(category);
        return count != null ? count.get() : 0L;
    }

    private void updatePopularProducts(AnalyticsEventDto analytics) {
        // 인기 상품 순위 업데이트 로직
        log.debug("Updating popular products ranking");
        
        // 실제로는 Redis Sorted Set 등을 활용하여 실시간 순위 관리
        if (analytics.getProductId() != null) {
            Long viewCount = getProductViewCount(analytics.getProductId());
            log.debug("Product {} has {} views", analytics.getProductId(), viewCount);
        }
    }

    private void updateRealtimeActivity(AnalyticsEventDto analytics) {
        // 실시간 활동 통계 업데이트
        log.debug("Updating real-time activity stats");
        
        // 예: 현재 온라인 사용자 수, 실시간 조회수 등
        // 실제로는 WebSocket이나 Server-Sent Events를 통해 
        // 프론트엔드에 실시간 데이터 전송
    }

    private void updateTopSearchKeywords(String keyword, Long count) {
        // 상위 검색어 목록 업데이트
        log.debug("Updating top search keywords: {} with count {}", keyword, count);
        
        // 실제로는 Redis Sorted Set이나 별도 캐시에서 관리
        // 시간대별, 일별, 주별 인기 검색어 통계 생성
    }

    // 인기 상품 조회
    public Map<String, Object> getPopularProducts(int limit) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 실제 상품 데이터 조회
            List<ProductResponse> allProducts = productRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .limit(limit)
                .map(product -> new ProductResponse(
                    product.getId(),
                    product.getTitle(),
                    product.getPrice(),
                    product.getCategory(),
                    product.getImageUrl(),
                    product.getStatus().toString()
                ))
                .collect(Collectors.toList());
            
            // 조회수 기반으로 정렬 (실제로는 복합 지표 사용)
            List<Map<String, Object>> popularProducts = allProducts.stream()
                .map(product -> {
                    Map<String, Object> productMap = new HashMap<>();
                    productMap.put("productId", product.getProductId());
                    productMap.put("title", product.getTitle());
                    productMap.put("price", product.getPrice());
                    productMap.put("category", product.getCategory());
                    productMap.put("imageUrl", product.getImageUrl());
                    productMap.put("viewCount", getProductViewCount(product.getProductId()));
                    return productMap;
                })
                .sorted((a, b) -> Long.compare((Long) b.get("viewCount"), (Long) a.get("viewCount")))
                .collect(Collectors.toList());
            
            response.put("products", popularProducts);
            response.put("message", "인기 상품 순위");
            response.put("limit", limit);
            response.put("totalCount", popularProducts.size());
            
        } catch (Exception e) {
            log.error("Failed to get popular products", e);
            response.put("error", "인기 상품 조회 중 오류가 발생했습니다.");
            response.put("products", new ArrayList<>());
        }
        
        return response;
    }

    // 대시보드 통계 조회
    public Map<String, Object> getDashboardStats() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 총 상품 수
            long totalProducts = productRepository.count();
            
            // 총 조회수 계산
            long totalViews = productViewCounts.values().stream()
                .mapToLong(AtomicLong::get)
                .sum();
            
            // 총 검색 수 계산
            long totalSearches = searchKeywordCounts.values().stream()
                .mapToLong(AtomicLong::get)
                .sum();
            
            // 카테고리별 상품 수
            Map<String, Long> categoryStats = productRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                    product -> product.getCategory() != null ? product.getCategory() : "기타",
                    Collectors.counting()
                ));
            
            // 인기 검색어 Top 5
            List<Map<String, Object>> topKeywords = searchKeywordCounts.entrySet().stream()
                .sorted(Map.Entry.<String, AtomicLong>comparingByValue((a, b) -> Long.compare(b.get(), a.get())))
                .limit(5)
                .map(entry -> {
                    Map<String, Object> keyword = new HashMap<>();
                    keyword.put("keyword", entry.getKey());
                    keyword.put("count", entry.getValue().get());
                    return keyword;
                })
                .collect(Collectors.toList());
            
            response.put("totalProducts", totalProducts);
            response.put("totalViews", totalViews);
            response.put("totalSearches", totalSearches);
            response.put("categoryStats", categoryStats);
            response.put("topKeywords", topKeywords);
            response.put("timestamp", System.currentTimeMillis());
            response.put("message", "실시간 대시보드 통계");
            
        } catch (Exception e) {
            log.error("Failed to get dashboard stats", e);
            response.put("error", "대시보드 통계 조회 중 오류가 발생했습니다.");
        }
        
        return response;
    }

    // 분석 리포트 생성을 위한 메서드들 (추가 기능)
    public void generateDailyReport() {
        log.info("Generating daily analytics report");
        // 일일 리포트 생성 로직
    }

    public void generateWeeklyReport() {
        log.info("Generating weekly analytics report");
        // 주간 리포트 생성 로직
    }

    public void generateMonthlyReport() {
        log.info("Generating monthly analytics report");
        // 월간 리포트 생성 로직
    }
} 