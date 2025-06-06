package com.minicarrot.product.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "carrot_products")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Product {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long id;
    
    @Column(nullable = false)
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(nullable = false)
    private BigDecimal price;
    
    private String category;
    
    @Column(name = "image_url")
    private String imageUrl;
    
    @Column(name = "seller_id", nullable = false)
    private Long sellerId;
    
    @Column(name = "seller_nickname")
    private String sellerNickname;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ProductStatus status = ProductStatus.AVAILABLE;
    
    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    // 비즈니스 로직용 생성자
    public Product(String title, String description, BigDecimal price, String category, 
                   String imageUrl, Long sellerId, String sellerNickname) {
        this.title = title;
        this.description = description;
        this.price = price;
        this.category = category;
        this.imageUrl = imageUrl;
        this.sellerId = sellerId;
        this.sellerNickname = sellerNickname;
        this.status = ProductStatus.AVAILABLE;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    // 비즈니스 메서드들
    public void markAsSold() {
        if (this.status == ProductStatus.SOLD) {
            throw new IllegalStateException("이미 판매된 상품입니다.");
        }
        this.status = ProductStatus.SOLD;
        this.updatedAt = LocalDateTime.now();
    }
    
    public void updateProduct(String title, String description, BigDecimal price, 
                             String category, String imageUrl) {
        if (this.status == ProductStatus.SOLD) {
            throw new IllegalStateException("판매된 상품은 수정할 수 없습니다.");
        }
        this.title = title;
        this.description = description;
        this.price = price;
        this.category = category;
        this.imageUrl = imageUrl;
        this.updatedAt = LocalDateTime.now();
    }
    
    public boolean isSold() {
        return this.status == ProductStatus.SOLD;
    }
    
    public boolean isOwnedBy(Long userId) {
        return this.sellerId.equals(userId);
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    public enum ProductStatus {
        AVAILABLE, SOLD
    }
} 