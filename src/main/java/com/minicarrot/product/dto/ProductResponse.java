package com.minicarrot.product.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {
    private Long productId;
    private String title;
    private BigDecimal price;
    private String category;
    private String imageUrl;
    private String status;
    private Long viewCount;
} 