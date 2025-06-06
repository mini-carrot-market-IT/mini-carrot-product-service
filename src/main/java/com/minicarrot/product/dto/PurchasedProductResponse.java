package com.minicarrot.product.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PurchasedProductResponse {
    private Long productId;
    private String title;
    private BigDecimal price;
    private String sellerNickname;
    private LocalDateTime purchasedAt;
    private String imageUrl;
} 