package com.minicarrot.product.dto.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsEventDto {
    private String eventId;
    private String eventType; // VIEW, SEARCH, PURCHASE, CLICK
    private Long userId;
    private String sessionId;
    private String userAgent;
    private String ipAddress;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
    
    // 상품 관련 정보
    private Long productId;
    private String productTitle;
    private String productCategory;
    private Integer productPrice;
    
    // 검색 관련 정보
    private String searchKeyword;
    private String searchCategory;
    private Integer searchResultCount;
    
    // 페이지 정보
    private String referrer;
    private String currentPage;
    private Long viewDuration;
    
    // 추가 메타데이터
    private Map<String, Object> metadata;
} 