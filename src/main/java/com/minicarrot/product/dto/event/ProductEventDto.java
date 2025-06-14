package com.minicarrot.product.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductEventDto {
    private Long productId;
    private String title;
    private String description;
    private Integer price;
    private String category;
    private String imageUrl;
    private Long sellerId;
    private String sellerNickname;
    private String status;
    private String eventType; // CREATED, UPDATED, DELETED, PURCHASED
    private LocalDateTime eventTime;
    private String eventId;
    
    // 구매 관련 추가 정보
    private Long buyerId;
    private String buyerNickname;
    private Integer purchasePrice;
} 