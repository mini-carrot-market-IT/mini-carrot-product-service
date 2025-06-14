package com.minicarrot.product.dto.event;

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
public class NotificationEventDto {
    private String notificationId;
    private String type; // EMAIL, PUSH, SMS
    private String recipient; // 받는 사람 (email, userId 등)
    private String title;
    private String message;
    private String templateName;
    private Map<String, Object> templateData;
    private LocalDateTime createdAt;
    private String priority; // HIGH, MEDIUM, LOW
    
    // 상품 관련 정보
    private Long productId;
    private String productTitle;
    private Integer productPrice;
    private String productImageUrl;
    
    // 발송자 정보
    private Long senderId;
    private String senderNickname;
} 