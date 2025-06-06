package com.minicarrot.product.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "carrot_purchases")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Purchase {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "purchase_id")
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;  // 구매자 ID
    
    @Column(name = "seller_id", nullable = false)
    private Long sellerId;  // 판매자 ID
    
    @Column(name = "product_id", nullable = false)
    private Long productId;
    
    @Column(name = "purchase_price", nullable = false)
    private BigDecimal purchasePrice;  // 구매 가격
    
    @Column(name = "purchased_at", nullable = false)
    @Builder.Default
    private LocalDateTime purchasedAt = LocalDateTime.now();
    
    // 비즈니스 로직용 생성자
    public Purchase(Long productId, Long userId, Long sellerId, BigDecimal purchasePrice) {
        this.productId = productId;
        this.userId = userId;
        this.sellerId = sellerId;
        this.purchasePrice = purchasePrice;
        this.purchasedAt = LocalDateTime.now();
    }
    
    // 비즈니스 메서드들
    public boolean isBuyerOf(Long userId) {
        return this.userId.equals(userId);
    }
    
    // 기존 코드와의 호환성을 위한 메서드들
    public Long getUserId() {
        return this.userId;  // buyer_id를 반환
    }
    
    public Long getSellerId() {
        return this.sellerId;  // 실제 seller_id 반환
    }
} 