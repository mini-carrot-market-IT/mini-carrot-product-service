package com.minicarrot.product.repository;

import com.minicarrot.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    
    // 카테고리별 상품 조회
    List<Product> findByCategory(String category);
    
    // 판매자별 상품 조회
    List<Product> findBySellerId(Long sellerId);
    
    // 상품 상태별 조회
    List<Product> findByStatus(Product.ProductStatus status);
    
    // 카테고리와 상태로 조회
    List<Product> findByCategoryAndStatus(String category, Product.ProductStatus status);
    
    // 모든 상품을 최신순으로 조회
    List<Product> findAllByOrderByCreatedAtDesc();
    
    // 카테고리별 상품을 최신순으로 조회
    List<Product> findByCategoryOrderByCreatedAtDesc(String category);
} 